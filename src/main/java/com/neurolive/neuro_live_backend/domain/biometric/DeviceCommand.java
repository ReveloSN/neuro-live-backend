package com.neurolive.neuro_live_backend.domain.biometric;

import java.time.LocalDateTime;

// Representa un comando listo para enviarse a un dispositivo.
public record DeviceCommand(
        Long deviceId,
        String macAddress,
        Long patientId,
        String command,
        LocalDateTime dispatchedAt,
        String fallBackConfig
) {
}
