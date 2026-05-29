package com.neurolive.neuro_live_backend.domain.biometric;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Verifica el calculo y validaciones de la linea base biometrica.
class BaseLineTest {

    @Test
    void shouldCalculateBaselineWhenEnoughBiometricSamplesExist() {
        BaseLine baseLine = new BaseLine(15L);
        Instant sessionStart = Instant.parse("2026-03-27T08:00:00Z");

        BaseLine calculatedBaseLine = baseLine.calculate(List.of(
                new BiometricData(80.0f, 97.0f, sessionStart),
                new BiometricData(82.0f, 98.0f, sessionStart.plusSeconds(60)),
                new BiometricData(84.0f, 97.0f, sessionStart.plusSeconds(120)),
                new BiometricData(86.0f, 99.0f, sessionStart.plusSeconds(180)),
                new BiometricData(88.0f, 98.0f, sessionStart.plusSeconds(240)),
                new BiometricData(90.0f, 99.0f, sessionStart.plusSeconds(300))
        ));

        assertSame(baseLine, calculatedBaseLine);
        assertTrue(baseLine.isReady());
        assertEquals(85.0f, baseLine.getAvgBpm(), 0.0001f);
        assertEquals(98.0f, baseLine.getAvgSpo2(), 0.0001f);
        assertEquals(sessionStart.plusSeconds(300), baseLine.getCalculatedAt());
    }

    @Test
    void shouldNotMarkBaselineAsReadyWhenThereIsInsufficientData() {
        BaseLine baseLine = new BaseLine(17L);
        Instant sessionStart = Instant.parse("2026-03-27T08:00:00Z");

        baseLine.calculate(List.of(
                new BiometricData(81.0f, 98.0f, sessionStart),
                new BiometricData(82.0f, 98.0f, sessionStart.plusSeconds(120)),
                new BiometricData(83.0f, 99.0f, sessionStart.plusSeconds(240))
        ));

        assertFalse(baseLine.isReady());
        assertEquals(0.0f, baseLine.getAvgBpm(), 0.0001f);
        assertEquals(0.0f, baseLine.getAvgSpo2(), 0.0001f);
        assertNull(baseLine.getCalculatedAt());
    }

    @Test
    void shouldRejectInvalidPatientId() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new BaseLine(0L)
        );

        assertEquals("Patient reference must be a positive identifier", exception.getMessage());
    }

    @Test
    void shouldRejectNegativeBpmOrSpo2Averages() {
        BaseLine baseLine = new BaseLine(21L);

        IllegalArgumentException bpmException = assertThrows(
                IllegalArgumentException.class,
                () -> baseLine.applyCalculation(-1.0f, 98.0f, Instant.now())
        );
        IllegalArgumentException spo2Exception = assertThrows(
                IllegalArgumentException.class,
                () -> baseLine.applyCalculation(82.0f, -1.0f, Instant.now())
        );

        assertEquals("Average BPM must be a finite non-negative value", bpmException.getMessage());
        assertEquals("Average SpO2 must be a finite non-negative value", spo2Exception.getMessage());
    }

    @Test
    void shouldSetCalculatedAtWhenCalculationSucceeds() {
        BaseLine baseLine = new BaseLine(25L);
        Instant sessionStart = Instant.parse("2026-03-27T09:30:00Z");

        baseLine.calculate(List.of(
                new BiometricData(70.0f, 97.0f, sessionStart),
                new BiometricData(71.0f, 97.0f, sessionStart.plusSeconds(60)),
                new BiometricData(72.0f, 98.0f, sessionStart.plusSeconds(120)),
                new BiometricData(73.0f, 98.0f, sessionStart.plusSeconds(180)),
                new BiometricData(74.0f, 98.0f, sessionStart.plusSeconds(240)),
                new BiometricData(75.0f, 99.0f, sessionStart.plusSeconds(300))
        ));

        assertEquals(sessionStart.plusSeconds(300), baseLine.getCalculatedAt());
    }

    @Test
    void shouldNotBeReadyBeforeCalculation() {
        BaseLine baseLine = new BaseLine(28L);

        assertFalse(baseLine.isReady());
        assertEquals(0.0f, baseLine.getAvgBpm(), 0.0001f);
        assertEquals(0.0f, baseLine.getAvgSpo2(), 0.0001f);
    }

    @Test
    void shouldRejectNullOrZeroPatientId() {
        assertThrows(IllegalArgumentException.class, () -> new BaseLine(null));
        assertThrows(IllegalArgumentException.class, () -> new BaseLine(0L));
    }

    @Test
    void shouldSupportStructuredWeightExtensionPointWithoutChangingCrisisLogic() {
        BaseLine baseLine = new BaseLine(27L);
        Instant sessionStart = Instant.parse("2026-03-27T11:00:00Z");

        baseLine.calculate(List.of(
                new BiometricData(60.0f, 96.0f, sessionStart),
                new BiometricData(100.0f, 100.0f, sessionStart.plusSeconds(300))
        ), sample -> sample.bpm() >= 100.0f ? 3.0d : 1.0d);

        assertTrue(baseLine.isReady());
        assertEquals(90.0f, baseLine.getAvgBpm(), 0.0001f);
        assertEquals(99.0f, baseLine.getAvgSpo2(), 0.0001f);
    }
}
