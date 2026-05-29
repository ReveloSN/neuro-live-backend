package com.neurolive.neuro_live_backend.domain.biometric;

import java.time.Instant;

// Representa un comando listo para enviarse a un dispositivo.
public record DeviceCommand(
        Long deviceId,
        String macAddress,
        Long patientId,
        String command,
        Instant dispatchedAt,
        String fallBackConfig
) {
}
