package com.neurolive.neuro_live_backend.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// Recibe el identificador fisico del ESP32 que se desea vincular al paciente.
public record DeviceLinkRequestDTO(
        @NotBlank(message = "Device MAC address is required")
        @Pattern(
                regexp = "^(?:[0-9A-Fa-f]{2}[:-]){5}[0-9A-Fa-f]{2}$",
                message = "Device MAC address must use the format AA:BB:CC:DD:EE:FF"
        )
        String deviceMac,
        String fallBackConfig,
        @Size(min = 6, max = 128, message = "Device token must contain between 6 and 128 characters")
        String deviceToken
) {
}
