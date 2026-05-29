package com.neurolive.neuro_live_backend.presentation.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;

// Representa la telemetria que entra al pipeline de negocio.
@JsonIgnoreProperties(ignoreUnknown = true)
public record TelemetryPayload(
        Long patientId,
        String deviceMac,
        Float bpm,
        Float spo2,
        Instant observedAt,
        Boolean sensorContact,
        String predictionState,
        Float predictionConfidence,
        String predictionReasoning
) {

    // Mantiene compatibilidad con payloads sin contacto del sensor.
    public TelemetryPayload(Long patientId,
                            String deviceMac,
                            Float bpm,
                            Float spo2,
                            Instant observedAt) {
        this(patientId, deviceMac, bpm, spo2, observedAt, null, null, null, null);
    }

    // Mantiene compatibilidad con payloads sin prediccion.
    public TelemetryPayload(Long patientId,
                            String deviceMac,
                            Float bpm,
                            Float spo2,
                            Instant observedAt,
                            Boolean sensorContact) {
        this(patientId, deviceMac, bpm, spo2, observedAt, sensorContact, null, null, null);
    }
}
