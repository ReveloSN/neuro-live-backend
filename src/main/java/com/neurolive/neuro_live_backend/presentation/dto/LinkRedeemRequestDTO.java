package com.neurolive.neuro_live_backend.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LinkRedeemRequestDTO(
        @NotBlank(message = "Link token is required")
        @Size(min = 8, max = 32, message = "Link token must contain between 8 and 32 characters")
        @Pattern(regexp = "^[A-Za-z0-9]+$", message = "Link token must be alphanumeric")
        String token
) {
}
