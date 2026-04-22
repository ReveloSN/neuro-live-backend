package com.neurolive.neuro_live_backend.presentation.dto;

import com.neurolive.neuro_live_backend.domain.user.UserLink;

import java.time.LocalDateTime;

public record LinkTokenResponseDTO(
        Long linkId,
        String token,
        LocalDateTime expiresAt,
        String status
) {

    public static LinkTokenResponseDTO from(UserLink userLink) {
        return new LinkTokenResponseDTO(
                userLink.getId(),
                userLink.getToken(),
                userLink.getExpiresAt(),
                userLink.getStatus().name()
        );
    }
}
