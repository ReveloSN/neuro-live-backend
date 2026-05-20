package com.neurolive.neuro_live_backend.presentation.dto;

import com.neurolive.neuro_live_backend.domain.user.AuditLog;

import java.time.LocalDateTime;

public record AuditLogDTO(
        Long id,
        Long userId,
        String action,
        Long targetPatientId,
        LocalDateTime timestamp,
        String entryHash
) {

    public static AuditLogDTO from(AuditLog log) {
        return new AuditLogDTO(
                log.getId(),
                log.getUserId(),
                log.getAction(),
                log.getTargetPatientId(),
                log.getTimestamp(),
                log.getEntryHash()
        );
    }
}
