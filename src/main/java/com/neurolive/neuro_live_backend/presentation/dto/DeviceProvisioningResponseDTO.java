package com.neurolive.neuro_live_backend.presentation.dto;

import com.neurolive.neuro_live_backend.domain.biometric.Device;

import java.time.Instant;

// Devuelve el token de aprovisionamiento una sola vez al vincular el dispositivo.
public record DeviceProvisioningResponseDTO(
        Long id,
        Long patientId,
        String macAddress,
        Boolean connected,
        Instant linkedAt,
        Instant lastConnection,
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
