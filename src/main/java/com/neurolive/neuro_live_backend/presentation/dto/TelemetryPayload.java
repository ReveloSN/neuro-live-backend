package com.neurolive.neuro_live_backend.presentation.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;

// Representa la telemetria que entra al pipeline de negocio.
@JsonIgnoreProperties(ignoreUnknown = true)
public record TelemetryPayload(
        Long patientId,
        String deviceMac,
        Float bpm,
        Float spo2,
        LocalDateTime observedAt,
        Boolean sensorContact
) {

    // Mantiene compatibilidad con payloads sin contacto del sensor.
    public TelemetryPayload(Long patientId,
                            String deviceMac,
                            Float bpm,
                            Float spo2,
                            LocalDateTime observedAt) {
        this(patientId, deviceMac, bpm, spo2, observedAt, null);
    }
}

