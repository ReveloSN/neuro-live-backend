package com.neurolive.neuro_live_backend.domain.biometric;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Verifica la creacion, asignacion y desactivacion del modelo de umbral biometrico.
class ActivationThresholdTest {

    @Test
    void shouldCreateThresholdWithValidValues() {
        ActivationThreshold threshold = new ActivationThreshold(60f, 110f, 93f, 0.25f);

        assertEquals(60f, threshold.getBpmMin());
        assertEquals(110f, threshold.getBpmMax());
        assertEquals(93f, threshold.getSpo2Min());
        assertEquals(0.25f, threshold.getErrorRateMax());
        assertTrue(threshold.getActive());
    }

    @Test
    void shouldAllowNullMetrics() {
        ActivationThreshold threshold = new ActivationThreshold(null, null, null, null);

        assertNull(threshold.getBpmMin());
        assertNull(threshold.getBpmMax());
    }

    @Test
    void shouldRejectNegativeBpmMin() {
        assertThrows(IllegalArgumentException.class,
                () -> new ActivationThreshold(-1f, 110f, 93f, 0.2f));
    }

    @Test
    void shouldRejectNegativeSpo2Min() {
        assertThrows(IllegalArgumentException.class,
                () -> new ActivationThreshold(60f, 110f, -5f, 0.2f));
    }

    @Test
    void assignToPatient_shouldSetPatientIdAndClearPersonalUserId() {
        ActivationThreshold threshold = new ActivationThreshold(60f, 110f, 93f, 0.2f);
        threshold.assignToPatient(10L, 20L);

        assertEquals(10L, threshold.getPatientId());
        assertEquals(20L, threshold.getDefinedByUserId());
        assertNull(threshold.getPersonalUserId());
    }

    @Test
    void assignToPersonalUser_shouldSetPersonalUserIdAndClearPatientId() {
        ActivationThreshold threshold = new ActivationThreshold(60f, 110f, 93f, 0.2f);
        threshold.assignToPersonalUser(40L, 40L);

        assertEquals(40L, threshold.getPersonalUserId());
        assertNull(threshold.getPatientId());
    }

    @Test
    void deactivate_shouldSetActiveFalse() {
        ActivationThreshold threshold = new ActivationThreshold(60f, 110f, 93f, 0.2f);
        threshold.deactivate();

        assertFalse(threshold.getActive());
    }

    @Test
    void activate_shouldSetActiveTrue() {
        ActivationThreshold threshold = new ActivationThreshold(60f, 110f, 93f, 0.2f);
        threshold.deactivate();
        threshold.activate();

        assertTrue(threshold.getActive());
    }
}
