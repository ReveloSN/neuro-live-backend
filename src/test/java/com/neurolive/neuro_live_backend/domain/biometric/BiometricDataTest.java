package com.neurolive.neuro_live_backend.domain.biometric;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

// Verifica la validacion del record BiometricData al construirse con datos invalidos.
class BiometricDataTest {

    @Test
    void shouldCreateWithValidValues() {
        LocalDateTime ts = LocalDateTime.of(2026, 3, 10, 8, 0);
        BiometricData data = new BiometricData(80f, 98f, ts);

        assertEquals(80f, data.bpm());
        assertEquals(98f, data.spo2());
        assertEquals(ts, data.timestamp());
    }

    @Test
    void shouldRejectNegativeBpm() {
        assertThrows(IllegalArgumentException.class,
                () -> new BiometricData(-1f, 98f, LocalDateTime.now()));
    }

    @Test
    void shouldRejectNegativeSpo2() {
        assertThrows(IllegalArgumentException.class,
                () -> new BiometricData(80f, -0.1f, LocalDateTime.now()));
    }

    @Test
    void shouldRejectInfiniteBpm() {
        assertThrows(IllegalArgumentException.class,
                () -> new BiometricData(Float.POSITIVE_INFINITY, 98f, LocalDateTime.now()));
    }

    @Test
    void shouldRejectNaNSpo2() {
        assertThrows(IllegalArgumentException.class,
                () -> new BiometricData(80f, Float.NaN, LocalDateTime.now()));
    }

    @Test
    void shouldRejectNullTimestamp() {
        assertThrows(IllegalArgumentException.class,
                () -> new BiometricData(80f, 98f, null));
    }
}
