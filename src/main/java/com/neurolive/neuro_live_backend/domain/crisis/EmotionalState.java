package com.neurolive.neuro_live_backend.domain.crisis;

import com.neurolive.neuro_live_backend.data.enums.StateEnum;

// Representa el estado actual de bienestar
public record EmotionalState(StateEnum state) {

    private static final String NORMAL_COLOR_KEY = "green";
    private static final String AT_RISK_COLOR_KEY = "yellow";
    private static final String CRISIS_COLOR_KEY = "red";

    public EmotionalState {
        if (state == null) {
            throw new IllegalArgumentException("Emotional state is required");
        }
    }

    public static EmotionalState from(StateEnum state) {
        return new EmotionalState(state);
    }

    public boolean isNormal() {
        return state == StateEnum.NORMAL;
    }

    public boolean isAtRisk() {
        return state == StateEnum.RISK_ELEVATED;
    }

    public boolean isCrisis() {
        return state == StateEnum.ACTIVE_CRISIS;
    }

    // Mantiene la semantica del monitoreo visual
    public String colorKey() {
        return switch (state) {
            case NORMAL -> NORMAL_COLOR_KEY;
            case RISK_ELEVATED -> AT_RISK_COLOR_KEY;
            case ACTIVE_CRISIS -> CRISIS_COLOR_KEY;
        };
    }
}
