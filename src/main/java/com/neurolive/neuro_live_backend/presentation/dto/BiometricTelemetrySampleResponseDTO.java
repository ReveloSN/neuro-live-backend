package com.neurolive.neuro_live_backend.presentation.dto;

import com.neurolive.neuro_live_backend.domain.biometric.BiometricTelemetrySample;

import java.time.LocalDateTime;

// Expone la muestra cruda mas reciente sin datos sensibles ni tokens.
public record BiometricTelemetrySampleResponseDTO(
        Long sampleId,
        Long patientId,
        String deviceMac,
        float bpm,
        float spo2,
        LocalDateTime observedAt,
        String predictionState,
        Float predictionConfidence,
        String predictionReasoning
) {

    public static BiometricTelemetrySampleResponseDTO from(BiometricTelemetrySample sample) {
        return new BiometricTelemetrySampleResponseDTO(
                sample.getId(),
                sample.getPatientId(),
                sample.getDeviceMac(),
                sample.getBpm(),
                sample.getSpo2(),
                sample.getObservedAt(),
                sample.getPredictionState(),
                sample.getPredictionConfidence(),
                sample.getPredictionReasoning()
        );
    }
}
