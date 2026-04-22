package com.neurolive.neuro_live_backend.presentation.dto;

import com.neurolive.neuro_live_backend.domain.user.UserLink;

import java.time.LocalDateTime;

public record UserLinkResponseDTO(
        Long id,
        Long patientId,
        Long linkedUserId,
        String linkType,
        String status,
        LocalDateTime createdAt,
        LocalDateTime expiresAt,
        LocalDateTime consumedAt
) {

    public static UserLinkResponseDTO from(UserLink userLink) {
        return new UserLinkResponseDTO(
                userLink.getId(),
                userLink.getPatientId(),
                userLink.getLinkedUserId(),
                userLink.getLinkType() == null ? null : userLink.getLinkType().name(),
                userLink.getStatus().name(),
                userLink.getCreatedAt(),
                userLink.getExpiresAt(),
                userLink.getConsumedAt()
        );
    }
}
