package com.neurolive.neuro_live_backend.entity;

import java.time.LocalDateTime;

public record DeviceCommand(
        Long deviceId,
        String macAddress,
        Long patientId,
        String command,
        LocalDateTime dispatchedAt,
        String fallBackConfig
) {
}
