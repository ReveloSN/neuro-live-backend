package com.neurolive.neuro_live_backend.business.service;

import com.neurolive.neuro_live_backend.business.patterns.CrisisMediator;
import com.neurolive.neuro_live_backend.domain.biometric.BaseLine;
import com.neurolive.neuro_live_backend.domain.biometric.BiometricData;
import com.neurolive.neuro_live_backend.domain.biometric.BiometricTelemetrySample;
import com.neurolive.neuro_live_backend.domain.biometric.Device;
import com.neurolive.neuro_live_backend.infrastructure.mqtt.TelemetryPayload;
import com.neurolive.neuro_live_backend.repository.BiometricTelemetrySampleRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
// Procesa la telemetria recibida desde MQTT
public class TelemetryIngestionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TelemetryIngestionService.class);

    private final BiometricTelemetrySampleRepository biometricTelemetrySampleRepository;
    private final DeviceService deviceService;
    private final BaseLineService baseLineService;
    private final ActivationThresholdService activationThresholdService;
    private final RiskAssessmentService riskAssessmentService;
    private final MonitoringConsentService monitoringConsentService;
    private final CrisisMediator crisisMediator;
    private final CrisisOutcomePersistenceService crisisOutcomePersistenceService;

    public TelemetryIngestionService(BiometricTelemetrySampleRepository biometricTelemetrySampleRepository,
            DeviceService deviceService,
            BaseLineService baseLineService,
            ActivationThresholdService activationThresholdService,
            RiskAssessmentService riskAssessmentService,
            MonitoringConsentService monitoringConsentService,
            CrisisMediator crisisMediator,
            CrisisOutcomePersistenceService crisisOutcomePersistenceService) {
        this.biometricTelemetrySampleRepository = biometricTelemetrySampleRepository;
        this.deviceService = deviceService;
        this.baseLineService = baseLineService;
        this.activationThresholdService = activationThresholdService;
        this.riskAssessmentService = riskAssessmentService;
        this.monitoringConsentService = monitoringConsentService;
        this.crisisMediator = crisisMediator;
        this.crisisOutcomePersistenceService = crisisOutcomePersistenceService;
    }

    public TelemetryIngestionResult ingest(TelemetryPayload payload) {
        if (payload == null) {
            throw new IllegalArgumentException("Telemetry payload is required");
        }

        Long patientId = validatePatientId(payload.patientId());
        monitoringConsentService.assertMonitoringAllowed(patientId);
        BiometricData biometricData = new BiometricData(
                requireMetric(payload.bpm(), "Telemetry BPM"),
                requireMetric(payload.spo2(), "Telemetry SpO2"),
                requireObservedAt(payload.observedAt()));

        Device device = validateLinkedDevice(patientId, payload.deviceMac());

        // Guarda la muestra biometrica antes de evaluar crisis
        LOGGER.debug(
                "Persisting telemetry sample patientId={} deviceMac={} bpm={} spo2={} observedAt={}",
                patientId,
                device.getMacAddress(),
                biometricData.bpm(),
                biometricData.spo2(),
                biometricData.timestamp()
        );
        BiometricTelemetrySample storedSample = biometricTelemetrySampleRepository.save(
                BiometricTelemetrySample.from(patientId, device.getMacAddress(), biometricData));
        LOGGER.debug(
                "Saved telemetry sample id={} patientId={} deviceMac={} observedAt={}",
                storedSample.getId(),
                storedSample.getPatientId(),
                storedSample.getDeviceMac(),
                storedSample.getObservedAt()
        );

        Device updatedDevice = deviceService.registerTelemetry(device.getMacAddress(), biometricData.timestamp());
        BaseLine baseLine = baseLineService.updateFromTelemetry(patientId, loadPatientTelemetry(patientId));
        var activationThreshold = activationThresholdService.resolveForPatient(patientId);
        RiskAssessmentService.AssessmentSnapshot assessmentSnapshot =
                riskAssessmentService.assess(patientId, biometricData, baseLine);

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
                            assessmentSnapshot.inferredState()));
            if (crisisMediationResult.crisisDetected()) {
                crisisOutcomePersistenceService.persist(crisisMediationResult);
                deviceService.sendCommand(
                        updatedDevice.getId(),
                        buildCommand(crisisMediationResult.interventionProtocol()),
                        LocalDateTime.now()
                );
            }
        }

        return new TelemetryIngestionResult(storedSample, updatedDevice, baseLine, crisisMediationResult);
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
        return biometricTelemetrySampleRepository.findAllByPatientIdOrderByObservedAtAsc(patientId).stream()
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

    private Long validatePatientId(Long patientId) {
        if (patientId == null || patientId <= 0) {
            throw new IllegalArgumentException("Patient reference must be a positive identifier");
        }
        return patientId;
    }

    private float requireMetric(Float value, String fieldName) {
        if (value == null || !Float.isFinite(value) || value < 0.0f) {
            throw new IllegalArgumentException(fieldName + " must be a finite non-negative value");
        }
        return value;
    }

    private LocalDateTime requireObservedAt(LocalDateTime observedAt) {
        if (observedAt == null) {
            throw new IllegalArgumentException("Telemetry observed time is required");
        }
        return observedAt;
    }
}
