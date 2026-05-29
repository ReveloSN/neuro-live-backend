package com.neurolive.neuro_live_backend.presentation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AccountRecoveryResetDTO(
        @NotBlank @Email String email,
        @NotBlank @Pattern(regexp = "^\\d{6}$", message = "Recovery code must be exactly 6 digits") String code,
        @NotBlank String newPassword
) {
}
