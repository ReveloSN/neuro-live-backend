package com.neurolive.neuro_live_backend.presentation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SAMResponseRequestDTO(
        @NotNull @Min(1) @Max(9) Integer valence,
        @NotNull @Min(1) @Max(9) Integer arousal
) {
}
