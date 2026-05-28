package com.neurolive.neuro_live_backend.business.service;

import com.neurolive.neuro_live_backend.business.patterns.CrisisMediator;
import com.neurolive.neuro_live_backend.business.patterns.PatientStateUpdate;
import com.neurolive.neuro_live_backend.data.enums.StateEnum;
import com.neurolive.neuro_live_backend.domain.biometric.BaseLine;
import com.neurolive.neuro_live_backend.domain.biometric.BiometricData;
import com.neurolive.neuro_live_backend.domain.biometric.BiometricTelemetrySample;
import com.neurolive.neuro_live_backend.domain.biometric.Device;
import com.neurolive.neuro_live_backend.infrastructure.config.AnalysisProperties;
import com.neurolive.neuro_live_backend.presentation.dto.TelemetryPayload;
import com.neurolive.neuro_live_backend.repository.BiometricTelemetrySampleRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
// Procesa la telemetria recibida desde el gateway realtime.
public class TelemetryIngestionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TelemetryIngestionService.class);
    private static final float MIN_BPM = 30.0f;
    private static final float MAX_BPM = 220.0f;
    private static final float MIN_SPO2 = 50.0f;
    private static final float MAX_SPO2 = 100.0f;
    private static final int MAX_PREDICTION_REASONING_LENGTH = 512;

    private final BiometricTelemetrySampleRepository biometricTelemetrySampleRepository;
    private final DeviceService deviceService;
    private final BaseLineService baseLineService;
    private final ActivationThresholdService activationThresholdService;
    private final RiskAssessmentService riskAssessmentService;
    private final MonitoringConsentService monitoringConsentService;
    private final CrisisMediator crisisMediator;
    private final CrisisOutcomePersistenceService crisisOutcomePersistenceService;
    private final AnalysisProperties analysisProperties;

    public TelemetryIngestionService(BiometricTelemetrySampleRepository biometricTelemetrySampleRepository,
            DeviceService deviceService,
            BaseLineService baseLineService,
            ActivationThresholdService activationThresholdService,
            RiskAssessmentService riskAssessmentService,
            MonitoringConsentService monitoringConsentService,
            CrisisMediator crisisMediator,
            CrisisOutcomePersistenceService crisisOutcomePersistenceService,
            AnalysisProperties analysisProperties) {
        this.biometricTelemetrySampleRepository = biometricTelemetrySampleRepository;
        this.deviceService = deviceService;
        this.baseLineService = baseLineService;
        this.activationThresholdService = activationThresholdService;
        this.riskAssessmentService = riskAssessmentService;
        this.monitoringConsentService = monitoringConsentService;
        this.crisisMediator = crisisMediator;
        this.crisisOutcomePersistenceService = crisisOutcomePersistenceService;
        this.analysisProperties = analysisProperties;
    }

    // Reutiliza el flujo de telemetria existente y ahora tambien reacciona a reconexiones y fallos del sensor.
    public TelemetryIngestionResult ingest(TelemetryPayload payload) {
        if (payload == null) {
            throw new IllegalArgumentException("Telemetry payload is required");
        }

        Long patientId = validatePatientId(payload.patientId());
        monitoringConsentService.assertMonitoringAllowed(patientId);
        BiometricData biometricData = new BiometricData(
                requireBpm(payload.bpm()),
                requireSpo2(payload.spo2()),
                requireObservedAt(payload.observedAt()));
        String predictionState = normalizePredictionState(payload.predictionState());
        Float predictionConfidence = clampPredictionConfidence(payload.predictionConfidence());
        String predictionReasoning = normalizePredictionReasoning(payload.predictionReasoning());

        Device device = validateLinkedDevice(patientId, payload.deviceMac());
        boolean wasConnected = Boolean.TRUE.equals(device.getIsConnected());
        Boolean previousSensorContact = device.getSensorContact();

        // Guarda la muestra biometrica antes de evaluar crisis
        LOGGER.debug(
                "Persisting telemetry sample patientId={} deviceMac={} bpm={} spo2={} observedAt={} sensorContact={}",
                patientId,
                device.getMacAddress(),
                biometricData.bpm(),
                biometricData.spo2(),
                biometricData.timestamp(),
                payload.sensorContact()
        );
        BiometricTelemetrySample storedSample = biometricTelemetrySampleRepository.save(
                BiometricTelemetrySample.from(
                        patientId,
                        device.getMacAddress(),
                        biometricData,
                        predictionState,
                        predictionConfidence,
                        predictionReasoning
                ));
        LOGGER.debug(
                "Saved telemetry sample id={} patientId={} deviceMac={} observedAt={}",
                storedSample.getId(),
                storedSample.getPatientId(),
                storedSample.getDeviceMac(),
                storedSample.getObservedAt()
        );

        Device updatedDevice = deviceService.registerTelemetry(
                device.getMacAddress(),
                biometricData.timestamp(),
                payload.sensorContact()
        );
        publishDeviceConnectivityUpdateIfNeeded(
                patientId,
                biometricData.timestamp(),
                wasConnected,
                previousSensorContact,
                updatedDevice
        );
        BaseLine baseLine = baseLineService.updateFromTelemetry(patientId, loadPatientTelemetry(patientId));
        var activationThreshold = activationThresholdService.resolveForPatient(patientId);
        RiskAssessmentService.AssessmentSnapshot assessmentSnapshot =
                riskAssessmentService.assess(patientId, biometricData, baseLine);
        StateEnum predictiveState = mapPredictiveState(predictionState);
        StateEnum combinedState = maxSeverity(assessmentSnapshot.inferredState(), predictiveState);
        if (combinedState.severity() > assessmentSnapshot.inferredState().severity()) {
            LOGGER.info(
                    "AI prediction elevated patient state patientId={} deviceMac={} predictionState={} confidence={} reactiveState={} finalState={}",
                    patientId,
                    device.getMacAddress(),
                    predictionState,
                    predictionConfidence,
                    assessmentSnapshot.inferredState(),
                    combinedState
            );
        }

        CrisisMediator.CrisisMediationResult crisisMediationResult = null;
        if (baseLine.isReady() || hasUsableThreshold(activationThreshold)) {
            crisisMediationResult = crisisMediator.mediate(
                    new CrisisMediator.CrisisEvaluationInput(
                            patientId,
                            biometricData,
                            baseLine,
                            activationThreshold,
                            assessmentSnapshot.errorRate(),
                            assessmentSnapshot.dwellTime(),
                            assessmentSnapshot.flightTime(),
                            assessmentSnapshot.errorCount(),
                            combinedState));
            if (crisisMediationResult.crisisDetected()) {
                crisisOutcomePersistenceService.persist(crisisMediationResult);
                trySendInterventionCommand(updatedDevice, crisisMediationResult);
            }
        }

        return new TelemetryIngestionResult(storedSample, updatedDevice, baseLine, crisisMediationResult);
    }

    @Transactional(readOnly = true)
    // Devuelve la ultima muestra guardada para que el dashboard no dependa solo de eventos STOMP.
    public BiometricTelemetrySample findLatestForPatient(Long patientId) {
        Long validatedPatientId = validatePatientId(patientId);
        return biometricTelemetrySampleRepository
                .findAllByPatientIdOrderByObservedAtDesc(validatedPatientId, PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("No telemetry samples found for patient " + validatedPatientId));
    }

    // Notifica al cuidador solo cuando cambia la conectividad o el contacto del sensor.
    private void publishDeviceConnectivityUpdateIfNeeded(Long patientId,
                                                         LocalDateTime observedAt,
                                                         boolean wasConnected,
                                                         Boolean previousSensorContact,
                                                         Device updatedDevice) {
        boolean reconnected = !wasConnected && Boolean.TRUE.equals(updatedDevice.getIsConnected());
        boolean sensorStatusChanged = !Objects.equals(previousSensorContact, updatedDevice.getSensorContact());

        if (!reconnected && !sensorStatusChanged) {
            return;
        }

        crisisMediator.publishUpdate(
                PatientStateUpdate.caregiverDeviceStatus(
                        patientId,
                        observedAt,
                        updatedDevice.getIsConnected(),
                        updatedDevice.getSensorContact()
                )
        );
    }

    private Device validateLinkedDevice(Long patientId, String deviceMac) {
        Device device;
        try {
            device = deviceService.findByMacAddress(deviceMac);
        } catch (EntityNotFoundException exception) {
            LOGGER.warn("Telemetry device was not found patientId={} deviceMac={}", patientId, deviceMac);
            throw exception;
        }

        LOGGER.debug(
                "Found telemetry device patientId={} deviceMac={} connected={} lastConnection={}",
                device.getPatientId(),
                device.getMacAddress(),
                device.getIsConnected(),
                device.getLastConnection()
        );

        if (!patientId.equals(device.getPatientId())) {
            throw new IllegalArgumentException("Telemetry patient does not match the linked device");
        }

        return device;
    }

    private List<BiometricData> loadPatientTelemetry(Long patientId) {
        LocalDateTime windowStart = LocalDateTime.now().minus(analysisProperties.telemetryWindow());
        LocalDateTime windowEnd = LocalDateTime.now();
        return biometricTelemetrySampleRepository
                .findAllByPatientIdAndObservedAtBetweenOrderByObservedAtAsc(patientId, windowStart, windowEnd)
                .stream()
                .map(BiometricTelemetrySample::toDomain)
                .toList();
    }

    private boolean hasUsableThreshold(com.neurolive.neuro_live_backend.domain.biometric.ActivationThreshold activationThreshold) {
        return activationThreshold != null
                && Boolean.TRUE.equals(activationThreshold.getActive())
                && (activationThreshold.getBpmMin() != null
                        || activationThreshold.getBpmMax() != null
                        || activationThreshold.getSpo2Min() != null
                        || activationThreshold.getErrorRateMax() != null);
    }

    private String buildCommand(com.neurolive.neuro_live_backend.domain.crisis.InterventionProtocol interventionProtocol) {
        if (interventionProtocol == null) {
            return "CALM_MODE";
        }

        // Convierte el protocolo clinico en la orden concreta que entienden actuadores y clientes conectados.
        return switch (interventionProtocol.getType().canonical()) {
            case UI -> "UI_INTERVENTION";
            case BREATHING -> "BREATHING_INTERVENTION";
            case LIGHT -> "LIGHT_INTERVENTION";
            case AUDIO -> "AUDIO_INTERVENTION";
            default -> "CALM_MODE";
        };
    }

    // Intenta enviar el comando sin deshacer la persistencia clinica.
    private void trySendInterventionCommand(Device updatedDevice,
                                            CrisisMediator.CrisisMediationResult crisisMediationResult) {
        try {
            deviceService.sendCommand(
                    updatedDevice.getId(),
                    buildCommand(crisisMediationResult.interventionProtocol()),
                    LocalDateTime.now()
            );
        } catch (RuntimeException exception) {
            LOGGER.warn(
                    "Command delivery failed but telemetry and crisis persistence continue deviceId={} reason={}",
                    updatedDevice.getMacAddress(),
                    exception.getMessage()
            );
        }
    }

    // Mapea la prediccion externa al enum clinico actual.
    private StateEnum mapPredictiveState(String predictionState) {
        if (predictionState == null || predictionState.isBlank()) {
            return StateEnum.NORMAL;
        }
        return switch (predictionState) {
            case "PRE_CRISIS" -> StateEnum.ACTIVE_CRISIS;
            case "WARNING" -> StateEnum.RISK_ELEVATED;
            case "STABLE", "INSUFFICIENT_DATA" -> StateEnum.NORMAL;
            default -> StateEnum.NORMAL;
        };
    }

    // Conserva la mayor severidad entre senales reactivas y predictivas.
    private StateEnum maxSeverity(StateEnum left, StateEnum right) {
        int rightSeverity = right == null ? 0 : right.severity();
        return rightSeverity > left.severity() ? right : left;
    }

    private Long validatePatientId(Long patientId) {
        if (patientId == null || patientId <= 0) {
            throw new IllegalArgumentException("Patient reference must be a positive identifier");
        }
        return patientId;
    }

    private float requireBpm(Float value) {
        return requireMetricInRange(value, "Telemetry BPM", MIN_BPM, MAX_BPM);
    }

    private float requireSpo2(Float value) {
        return requireMetricInRange(value, "Telemetry SpO2", MIN_SPO2, MAX_SPO2);
    }

    private float requireMetricInRange(Float value, String fieldName, float min, float max) {
        if (value == null || !Float.isFinite(value)) {
            throw new IllegalArgumentException(fieldName + " must be finite");
        }
        if (value < min || value > max) {
            throw new IllegalArgumentException(fieldName + " must be between "
                    + (int) min
                    + " and "
                    + (int) max);
        }
        return value;
    }

    private LocalDateTime requireObservedAt(LocalDateTime observedAt) {
        if (observedAt == null) {
            throw new IllegalArgumentException("Telemetry observed time is required");
        }
        return observedAt;
    }

    private String normalizePredictionState(String predictionState) {
        if (predictionState == null || predictionState.isBlank()) {
            return null;
        }
        String normalized = predictionState.trim().toUpperCase(java.util.Locale.ROOT);
        if (!java.util.Set.of("STABLE", "WARNING", "PRE_CRISIS", "INSUFFICIENT_DATA").contains(normalized)) {
            throw new IllegalArgumentException("Prediction state is not supported");
        }
        return normalized;
    }

    private Float clampPredictionConfidence(Float predictionConfidence) {
        if (predictionConfidence == null) {
            return null;
        }
        if (!Float.isFinite(predictionConfidence)) {
            throw new IllegalArgumentException("Prediction confidence must be finite");
        }
        return Math.max(0.0f, Math.min(1.0f, predictionConfidence));
    }

    private String normalizePredictionReasoning(String predictionReasoning) {
        if (predictionReasoning == null) {
            return null;
        }
        String normalized = predictionReasoning.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        return normalized.length() <= MAX_PREDICTION_REASONING_LENGTH
                ? normalized
                : normalized.substring(0, MAX_PREDICTION_REASONING_LENGTH);
    }
}
