package com.neurolive.neuro_live_backend.business.service;

import com.neurolive.neuro_live_backend.business.patterns.CrisisMediator;
import com.neurolive.neuro_live_backend.domain.biometric.ActivationThreshold;
import com.neurolive.neuro_live_backend.domain.biometric.BaseLine;
import com.neurolive.neuro_live_backend.domain.biometric.BiometricData;
import com.neurolive.neuro_live_backend.domain.biometric.BiometricTelemetrySample;
import com.neurolive.neuro_live_backend.domain.biometric.Device;
import com.neurolive.neuro_live_backend.infrastructure.mqtt.TelemetryPayload;
import com.neurolive.neuro_live_backend.repository.ActivationThresholdRepository;
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
    private final ActivationThresholdRepository activationThresholdRepository;
    private final CrisisMediator crisisMediator;
    private final CrisisOutcomePersistenceService crisisOutcomePersistenceService;

    public TelemetryIngestionService(BiometricTelemetrySampleRepository biometricTelemetrySampleRepository,
            DeviceService deviceService,
            BaseLineService baseLineService,
            ActivationThresholdRepository activationThresholdRepository,
            CrisisMediator crisisMediator,
            CrisisOutcomePersistenceService crisisOutcomePersistenceService) {
        this.biometricTelemetrySampleRepository = biometricTelemetrySampleRepository;
        this.deviceService = deviceService;
        this.baseLineService = baseLineService;
        this.activationThresholdRepository = activationThresholdRepository;
        this.crisisMediator = crisisMediator;
        this.crisisOutcomePersistenceService = crisisOutcomePersistenceService;
    }

    public TelemetryIngestionResult ingest(TelemetryPayload payload) {
        if (payload == null) {
            throw new IllegalArgumentException("Telemetry payload is required");
        }

        Long patientId = validatePatientId(payload.patientId());
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
        ActivationThreshold activationThreshold = resolveActivationThreshold();

        CrisisMediator.CrisisMediationResult crisisMediationResult = null;
        if (baseLine.isReady() || hasUsableThreshold(activationThreshold)) {
            // Consulta linea base y umbrales disponibles
            crisisMediationResult = crisisMediator.mediate(
                    new CrisisMediator.CrisisEvaluationInput(
                            patientId,
                            biometricData,
                            baseLine,
                            activationThreshold,
                            null));
            if (crisisMediationResult.crisisDetected()) {
                crisisOutcomePersistenceService.persist(crisisMediationResult);
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

    private ActivationThreshold resolveActivationThreshold() {
        return activationThresholdRepository.findFirstByActiveTrueOrderByCreatedAtDesc()
                .orElse(null);
    }

    private boolean hasUsableThreshold(ActivationThreshold activationThreshold) {
        return activationThreshold != null
                && Boolean.TRUE.equals(activationThreshold.getActive())
                && (activationThreshold.getBpmMin() != null
                        || activationThreshold.getBpmMax() != null
                        || activationThreshold.getSpo2Min() != null
                        || activationThreshold.getErrorRateMax() != null);
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
