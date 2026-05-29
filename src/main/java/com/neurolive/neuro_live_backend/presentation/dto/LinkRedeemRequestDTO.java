package com.neurolive.neuro_live_backend.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LinkRedeemRequestDTO(
        @NotBlank(message = "Link token is required")
        @Size(min = 6, max = 6, message = "Link token must contain 6 characters")
        @Pattern(regexp = "^[A-HJ-NP-Z2-9a-hj-np-z]+$", message = "Link token contains unsupported characters")
        String token
) {
}
