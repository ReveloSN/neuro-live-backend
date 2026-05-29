package com.neurolive.neuro_live_backend.presentation.dto;

import com.neurolive.neuro_live_backend.domain.user.UserLink;

import java.time.LocalDateTime;

// Respuesta segura de revocacion: no expone token ni datos clinicos.
public record LinkRevocationResponseDTO(
        Long linkId,
        String status,
        LocalDateTime revokedAt,
        String message
) {

    public static LinkRevocationResponseDTO from(UserLink userLink) {
        return new LinkRevocationResponseDTO(
                userLink.getId(),
                userLink.getStatus().name(),
                userLink.getRevokedAt(),
                "Link revoked successfully"
        );
    }
}
