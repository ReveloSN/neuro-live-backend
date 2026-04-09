package com.neurolive.neuro_live_backend.presentation.dto;

import com.neurolive.neuro_live_backend.business.service.AccountRecoveryService;

import java.time.LocalDateTime;

public record AccountRecoveryResponseDTO(
        String message,
        LocalDateTime expiresAt,
        boolean valid
) {

    public static AccountRecoveryResponseDTO from(AccountRecoveryService.RecoveryStatus recoveryStatus) {
        return new AccountRecoveryResponseDTO(
                recoveryStatus.message(),
                recoveryStatus.expiresAt(),
                recoveryStatus.valid()
        );
    }
}
