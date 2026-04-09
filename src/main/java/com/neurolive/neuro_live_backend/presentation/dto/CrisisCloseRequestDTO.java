package com.neurolive.neuro_live_backend.presentation.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CrisisCloseRequestDTO(
        @NotNull String finalState,
        LocalDateTime endedAt
) {
}
