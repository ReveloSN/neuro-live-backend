package com.neurolive.neuro_live_backend.presentation.dto;

import com.neurolive.neuro_live_backend.domain.biometric.BiometricTelemetrySample;

import java.time.LocalDateTime;

public record BiometricTelemetryLatestResponseDTO(
        Long sampleId,
        Long patientId,
        String deviceId,
        float bpm,
        float spo2,
        Boolean sensorConnected,
        LocalDateTime observedAt,
        LocalDateTime receivedAt,
        String predictionState,
        Float predictionConfidence,
        String predictionReasoning
) {

    public static BiometricTelemetryLatestResponseDTO from(BiometricTelemetrySample sample) {
        return new BiometricTelemetryLatestResponseDTO(
                sample.getId(),
                sample.getPatientId(),
                sample.getDeviceMac(),
                sample.getBpm(),
                sample.getSpo2(),
                sample.getSensorContact(),
                sample.getObservedAt(),
                sample.getObservedAt(),
                sample.getPredictionState(),
                sample.getPredictionConfidence(),
                sample.getPredictionReasoning()
        );
    }
}
