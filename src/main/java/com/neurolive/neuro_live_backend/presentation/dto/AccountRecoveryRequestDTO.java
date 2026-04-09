package com.neurolive.neuro_live_backend.presentation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AccountRecoveryRequestDTO(
        @NotBlank @Email String email
) {
}
