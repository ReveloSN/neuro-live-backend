package com.neurolive.neuro_live_backend.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;

public record BiometricDataDTO(
        @NotNull @Positive Long patientId,
        @NotBlank String deviceMac,
        @NotNull @Positive Float bpm,
        @NotNull @Positive Float spo2,
        @NotNull Instant observedAt,
        Boolean sensorContact,
        String predictionState,
        Float predictionConfidence,
        String predictionReasoning
) {

    // Convierte el DTO REST al mismo payload que usa la ingesta interna.
    public TelemetryPayload toPayload() {
        return new TelemetryPayload(patientId, deviceMac, bpm, spo2, observedAt, sensorContact,
                predictionState, predictionConfidence, predictionReasoning);
    }
}
