package com.neurolive.neuro_live_backend.infrastructure.mqtt;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TelemetryPayload(
        Long patientId,
        String deviceMac,
        Float bpm,
        Float spo2,
        LocalDateTime observedAt,
        Boolean sensorContact
) {

    // Mantiene compatibilidad con payloads existentes que no reportan contacto del sensor.
    public TelemetryPayload(Long patientId,
                            String deviceMac,
                            Float bpm,
                            Float spo2,
                            LocalDateTime observedAt) {
        this(patientId, deviceMac, bpm, spo2, observedAt, null);
    }
}
