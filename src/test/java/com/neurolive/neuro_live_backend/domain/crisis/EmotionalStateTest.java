package com.neurolive.neuro_live_backend.domain.crisis;

import com.neurolive.neuro_live_backend.data.enums.StateEnum;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmotionalStateTest {

    @Test
    void shouldCreateAValidEmotionalState() {
        EmotionalState emotionalState = EmotionalState.from(StateEnum.NORMAL);

        assertEquals(StateEnum.NORMAL, emotionalState.state());
    }

    @Test
    void shouldRejectNullState() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> EmotionalState.from(null)
        );

        assertEquals("Emotional state is required", exception.getMessage());
    }

    @Test
    void shouldIdentifyNormalStateCorrectly() {
        EmotionalState emotionalState = EmotionalState.from(StateEnum.NORMAL);

        assertTrue(emotionalState.isNormal());
        assertFalse(emotionalState.isAtRisk());
        assertFalse(emotionalState.isCrisis());
    }

    @Test
    void shouldIdentifyAtRiskStateCorrectly() {
        EmotionalState emotionalState = EmotionalState.from(StateEnum.RISK_ELEVATED);

        assertFalse(emotionalState.isNormal());
        assertTrue(emotionalState.isAtRisk());
        assertFalse(emotionalState.isCrisis());
    }

    @Test
    void shouldIdentifyCrisisStateCorrectly() {
        EmotionalState emotionalState = EmotionalState.from(StateEnum.ACTIVE_CRISIS);

        assertFalse(emotionalState.isNormal());
        assertFalse(emotionalState.isAtRisk());
        assertTrue(emotionalState.isCrisis());
    }

    @Test
    void shouldExposeMonitoringColorKey() {
        assertEquals("green", EmotionalState.from(StateEnum.NORMAL).colorKey());
        assertEquals("yellow", EmotionalState.from(StateEnum.RISK_ELEVATED).colorKey());
        assertEquals("red", EmotionalState.from(StateEnum.ACTIVE_CRISIS).colorKey());
    }
}
