package com.neurolive.neuro_live_backend.presentation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AccountRecoveryResetDTO(
        @NotBlank @Email String email,
        @NotBlank String token,
        @NotBlank String newPassword
) {
}
