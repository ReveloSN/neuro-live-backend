package com.neurolive.neuro_live_backend.presentation.dto;

import com.neurolive.neuro_live_backend.domain.analysis.KeystrokeDynamics;

import java.time.LocalDateTime;

public record KeystrokeCaptureResponseDTO(
        Long id,
        Long userId,
        String emotionalState,
        Float errorRate,
        LocalDateTime timestamp
) {

    public static KeystrokeCaptureResponseDTO from(KeystrokeDynamics keystrokeDynamics) {
        return new KeystrokeCaptureResponseDTO(
                keystrokeDynamics.getId(),
                keystrokeDynamics.getUserId(),
                keystrokeDynamics.analyzePattern().state().name(),
                keystrokeDynamics.getErrorRate(),
                keystrokeDynamics.getTimestamp()
        );
    }
}
