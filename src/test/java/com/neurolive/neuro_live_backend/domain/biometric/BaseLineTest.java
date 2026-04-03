package com.neurolive.neuro_live_backend.domain.biometric;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
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
        LocalDateTime sessionStart = LocalDateTime.of(2026, 3, 27, 8, 0);

        BaseLine calculatedBaseLine = baseLine.calculate(List.of(
                new BiometricData(80.0f, 97.0f, sessionStart),
                new BiometricData(82.0f, 98.0f, sessionStart.plusMinutes(1)),
                new BiometricData(84.0f, 97.0f, sessionStart.plusMinutes(2)),
                new BiometricData(86.0f, 99.0f, sessionStart.plusMinutes(3)),
                new BiometricData(88.0f, 98.0f, sessionStart.plusMinutes(4)),
                new BiometricData(90.0f, 99.0f, sessionStart.plusMinutes(5))
        ));

        assertSame(baseLine, calculatedBaseLine);
        assertTrue(baseLine.isReady());
        assertEquals(85.0f, baseLine.getAvgBpm(), 0.0001f);
        assertEquals(98.0f, baseLine.getAvgSpo2(), 0.0001f);
        assertEquals(sessionStart.plusMinutes(5), baseLine.getCalculatedAt());
    }

    @Test
    void shouldNotMarkBaselineAsReadyWhenThereIsInsufficientData() {
        BaseLine baseLine = new BaseLine(17L);
        LocalDateTime sessionStart = LocalDateTime.of(2026, 3, 27, 8, 0);

        baseLine.calculate(List.of(
                new BiometricData(81.0f, 98.0f, sessionStart),
                new BiometricData(82.0f, 98.0f, sessionStart.plusMinutes(2)),
                new BiometricData(83.0f, 99.0f, sessionStart.plusMinutes(4))
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
                () -> baseLine.applyCalculation(-1.0f, 98.0f, LocalDateTime.now())
        );
        IllegalArgumentException spo2Exception = assertThrows(
                IllegalArgumentException.class,
                () -> baseLine.applyCalculation(82.0f, -1.0f, LocalDateTime.now())
        );

        assertEquals("Average BPM must be a finite non-negative value", bpmException.getMessage());
        assertEquals("Average SpO2 must be a finite non-negative value", spo2Exception.getMessage());
    }

    @Test
    void shouldSetCalculatedAtWhenCalculationSucceeds() {
        BaseLine baseLine = new BaseLine(25L);
        LocalDateTime sessionStart = LocalDateTime.of(2026, 3, 27, 9, 30);

        baseLine.calculate(List.of(
                new BiometricData(70.0f, 97.0f, sessionStart),
                new BiometricData(71.0f, 97.0f, sessionStart.plusMinutes(1)),
                new BiometricData(72.0f, 98.0f, sessionStart.plusMinutes(2)),
                new BiometricData(73.0f, 98.0f, sessionStart.plusMinutes(3)),
                new BiometricData(74.0f, 98.0f, sessionStart.plusMinutes(4)),
                new BiometricData(75.0f, 99.0f, sessionStart.plusMinutes(5))
        ));

        assertEquals(sessionStart.plusMinutes(5), baseLine.getCalculatedAt());
    }

    @Test
    void shouldSupportStructuredWeightExtensionPointWithoutChangingCrisisLogic() {
        BaseLine baseLine = new BaseLine(27L);
        LocalDateTime sessionStart = LocalDateTime.of(2026, 3, 27, 11, 0);

        baseLine.calculate(List.of(
                new BiometricData(60.0f, 96.0f, sessionStart),
                new BiometricData(100.0f, 100.0f, sessionStart.plusMinutes(5))
        ), sample -> sample.bpm() >= 100.0f ? 3.0d : 1.0d);

        assertTrue(baseLine.isReady());
        assertEquals(90.0f, baseLine.getAvgBpm(), 0.0001f);
        assertEquals(99.0f, baseLine.getAvgSpo2(), 0.0001f);
    }
}
