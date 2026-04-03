package com.neurolive.neuro_live_backend.domain.analysis;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeystrokeDynamicsTest {

    @Test
    void shouldCreateAValidKeystrokeDynamicsRecord() {
        LocalDateTime timestamp = LocalDateTime.of(2026, 4, 2, 11, 0);

        KeystrokeDynamics keystrokeDynamics = KeystrokeDynamics.capture(121L, 95.0f, 110.0f, timestamp);

        assertEquals(121L, keystrokeDynamics.getUserId());
        assertEquals(95.0f, keystrokeDynamics.getDwellTime());
        assertEquals(110.0f, keystrokeDynamics.getFlightTime());
        assertEquals(timestamp, keystrokeDynamics.getTimestamp());
    }

    @Test
    void shouldRejectNegativeDwellTime() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> KeystrokeDynamics.capture(122L, -1.0f, 120.0f, LocalDateTime.now())
        );

        assertEquals("Dwell time must be a finite non-negative value", exception.getMessage());
    }

    @Test
    void shouldRejectNegativeFlightTime() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> KeystrokeDynamics.capture(123L, 80.0f, -5.0f, LocalDateTime.now())
        );

        assertEquals("Flight time must be a finite non-negative value", exception.getMessage());
    }

    @Test
    void shouldSetTimestampSafely() {
        KeystrokeDynamics keystrokeDynamics = KeystrokeDynamics.capture(124L, 90.0f, 115.0f, null);

        assertNotNull(keystrokeDynamics.getTimestamp());
    }

    @Test
    void shouldReturnAValidEmotionalStateOrientedAnalysisResult() {
        KeystrokeDynamics atRiskSignal = KeystrokeDynamics.capture(
                125L,
                190.0f,
                210.0f,
                LocalDateTime.of(2026, 4, 2, 11, 10)
        );
        KeystrokeDynamics crisisSignal = KeystrokeDynamics.capture(
                126L,
                280.0f,
                180.0f,
                LocalDateTime.of(2026, 4, 2, 11, 12)
        );

        assertTrue(atRiskSignal.analyzePattern().isAtRisk());
        assertTrue(crisisSignal.analyzePattern().isCrisis());
    }
}
