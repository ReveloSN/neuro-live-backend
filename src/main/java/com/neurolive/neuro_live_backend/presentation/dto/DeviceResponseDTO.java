package com.neurolive.neuro_live_backend.presentation.dto;

import com.neurolive.neuro_live_backend.domain.biometric.Device;

import java.time.LocalDateTime;

// Devuelve el estado del dispositivo sin exponer tokens sensibles.
public record DeviceResponseDTO(
        Long id,
        Long patientId,
        String macAddress,
        Boolean connected,
        LocalDateTime linkedAt,
        LocalDateTime lastConnection,
        Boolean sensorContact,
        String fallBackConfig
) {

    public static DeviceResponseDTO from(Device device) {
        return new DeviceResponseDTO(
                device.getId(),
                device.getPatientId(),
                device.getMacAddress(),
                device.getIsConnected(),
                device.getLinkedAt(),
                device.getLastConnection(),
                device.getSensorContact(),
                device.getFallBackConfig()
        );
    }
}
