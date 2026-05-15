package com.neurolive.neuro_live_backend.presentation.dto;

import com.neurolive.neuro_live_backend.domain.biometric.Device;

import java.time.LocalDateTime;

// Devuelve el token de aprovisionamiento una sola vez al vincular el dispositivo.
public record DeviceProvisioningResponseDTO(
        Long id,
        Long patientId,
        String macAddress,
        Boolean connected,
        LocalDateTime linkedAt,
        LocalDateTime lastConnection,
        Boolean sensorContact,
        String fallBackConfig,
        String deviceToken
) {

    public static DeviceProvisioningResponseDTO from(Device device) {
        return new DeviceProvisioningResponseDTO(
                device.getId(),
                device.getPatientId(),
                device.getMacAddress(),
                device.getIsConnected(),
                device.getLinkedAt(),
                device.getLastConnection(),
                device.getSensorContact(),
                device.getFallBackConfig(),
                device.getProvisioningToken()
        );
    }
}
