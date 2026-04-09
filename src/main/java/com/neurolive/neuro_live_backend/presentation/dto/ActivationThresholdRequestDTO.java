package com.neurolive.neuro_live_backend.presentation.dto;

import jakarta.validation.constraints.PositiveOrZero;

public record ActivationThresholdRequestDTO(
        @PositiveOrZero Float bpmMin,
        @PositiveOrZero Float bpmMax,
        @PositiveOrZero Float spo2Min,
        @PositiveOrZero Float errorRateMax
) {
}
