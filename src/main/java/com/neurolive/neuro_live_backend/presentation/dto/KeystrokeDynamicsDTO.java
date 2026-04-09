package com.neurolive.neuro_live_backend.presentation.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDateTime;

public record KeystrokeDynamicsDTO(
        @NotNull @Positive Long userId,
        String sessionId,
        @NotNull @Positive Float dwellTime,
        @NotNull @Positive Float flightTime,
        @PositiveOrZero Integer errorCount,
        @PositiveOrZero Float errorRate,
        LocalDateTime timestamp
) {
}
