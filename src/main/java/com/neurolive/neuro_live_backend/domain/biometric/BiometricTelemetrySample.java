package com.neurolive.neuro_live_backend.domain.biometric;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "biometric_telemetry_samples")
@Getter
@NoArgsConstructor
// Guarda cada muestra biometrica recibida para el analisis posterior.
public class BiometricTelemetrySample {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_id", nullable = false, updatable = false)
    private Long patientId;

    @Column(name = "device_mac", nullable = false, length = 17, updatable = false)
    private String deviceMac;

    @Column(nullable = false, updatable = false)
    private float bpm;

    @Column(nullable = false, updatable = false)
    private float spo2;

    @Column(name = "observed_at", nullable = false, updatable = false)
    private LocalDateTime observedAt;

    @Column(name = "prediction_state", length = 30, updatable = false)
    private String predictionState;

    @Column(name = "prediction_confidence", updatable = false)
    private Float predictionConfidence;

    @Column(name = "prediction_reasoning", length = 512, updatable = false)
    private String predictionReasoning;

    private BiometricTelemetrySample(Long patientId, String deviceMac, BiometricData biometricData) {
        this(patientId, deviceMac, biometricData, null, null, null);
    }

    private BiometricTelemetrySample(Long patientId,
                                     String deviceMac,
                                     BiometricData biometricData,
                                     String predictionState,
                                     Float predictionConfidence,
                                     String predictionReasoning) {
        this.patientId = validatePatientId(patientId);
        this.deviceMac = normalizeDeviceMac(deviceMac);
        this.bpm = biometricData.bpm();
        this.spo2 = biometricData.spo2();
        this.observedAt = biometricData.timestamp();
        this.predictionState = normalizePredictionState(predictionState);
        this.predictionConfidence = validatePredictionConfidence(predictionConfidence);
        this.predictionReasoning = normalizePredictionReasoning(predictionReasoning);
    }

    public static BiometricTelemetrySample from(Long patientId, String deviceMac, BiometricData biometricData) {
        if (biometricData == null) {
            throw new IllegalArgumentException("Biometric data is required");
        }
        return new BiometricTelemetrySample(patientId, deviceMac, biometricData);
    }

    public static BiometricTelemetrySample from(Long patientId,
                                                String deviceMac,
                                                BiometricData biometricData,
                                                String predictionState,
                                                Float predictionConfidence,
                                                String predictionReasoning) {
        if (biometricData == null) {
            throw new IllegalArgumentException("Biometric data is required");
        }
        return new BiometricTelemetrySample(
                patientId,
                deviceMac,
                biometricData,
                predictionState,
                predictionConfidence,
                predictionReasoning
        );
    }

    public BiometricData toDomain() {
        return new BiometricData(bpm, spo2, observedAt);
    }

    private Long validatePatientId(Long patientId) {
        if (patientId == null || patientId <= 0) {
            throw new IllegalArgumentException("Patient reference must be a positive identifier");
        }
        return patientId;
    }

    private String normalizeDeviceMac(String deviceMac) {
        if (deviceMac == null || deviceMac.isBlank()) {
            throw new IllegalArgumentException("Device MAC address is required");
        }

        return deviceMac.trim()
                .replace('-', ':')
                .toUpperCase();
    }

    private String normalizePredictionState(String predictionState) {
        if (predictionState == null || predictionState.isBlank()) {
            return null;
        }
        return predictionState.trim().toUpperCase(java.util.Locale.ROOT);
    }

    private Float validatePredictionConfidence(Float predictionConfidence) {
        if (predictionConfidence == null) {
            return null;
        }
        if (!Float.isFinite(predictionConfidence)) {
            throw new IllegalArgumentException("Prediction confidence must be finite");
        }
        return predictionConfidence;
    }

    private String normalizePredictionReasoning(String predictionReasoning) {
        if (predictionReasoning == null) {
            return null;
        }
        String normalized = predictionReasoning.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        return normalized.length() <= 512 ? normalized : normalized.substring(0, 512);
    }
}
