package com.neurolive.neuro_live_backend.presentation.dto;

import com.neurolive.neuro_live_backend.presentation.dto.TelemetryPayload;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

public record BiometricDataDTO(
        @NotNull @Positive Long patientId,
        @NotBlank String deviceMac,
        @NotNull @Positive Float bpm,
        @NotNull @Positive Float spo2,
        @NotNull LocalDateTime observedAt,
        Boolean sensorContact
) {

    // Convierte el DTO REST al mismo payload que usa la ingesta interna.
    public TelemetryPayload toPayload() {
        return new TelemetryPayload(patientId, deviceMac, bpm, spo2, observedAt, sensorContact);
    }
}
