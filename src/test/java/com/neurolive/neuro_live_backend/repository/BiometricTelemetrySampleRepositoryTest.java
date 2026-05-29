package com.neurolive.neuro_live_backend.repository;

import com.neurolive.neuro_live_backend.NeuroLiveBackendApplication;
import com.neurolive.neuro_live_backend.domain.biometric.BiometricData;
import com.neurolive.neuro_live_backend.domain.biometric.BiometricTelemetrySample;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = NeuroLiveBackendApplication.class)
@Transactional
class BiometricTelemetrySampleRepositoryTest {

    @Autowired
    private BiometricTelemetrySampleRepository biometricTelemetrySampleRepository;

    @Test
    void shouldReturnLatestPersistedSampleWithSensorAndPredictionFields() {
        BiometricTelemetrySample earlier = BiometricTelemetrySample.from(
                7L,
                "AA:BB:CC:DD:EE:FF",
                new BiometricData(90.0f, 97.0f, LocalDateTime.of(2026, 5, 29, 0, 10)),
                Boolean.FALSE,
                "WARNING",
                0.8f,
                "Earlier sample"
        );
        BiometricTelemetrySample latest = BiometricTelemetrySample.from(
                7L,
                "AA:BB:CC:DD:EE:FF",
                new BiometricData(95.0f, 98.0f, LocalDateTime.of(2026, 5, 29, 0, 15)),
                Boolean.TRUE,
                "INSUFFICIENT_DATA",
                0.2f,
                "Manual direct backend test"
        );

        biometricTelemetrySampleRepository.save(earlier);
        biometricTelemetrySampleRepository.save(latest);

        BiometricTelemetrySample storedLatest = biometricTelemetrySampleRepository
                .findFirstByPatientIdOrderByObservedAtDesc(7L)
                .orElseThrow();

        assertEquals(95.0f, storedLatest.getBpm());
        assertEquals(98.0f, storedLatest.getSpo2());
        assertEquals(Boolean.TRUE, storedLatest.getSensorContact());
        assertEquals("INSUFFICIENT_DATA", storedLatest.getPredictionState());
        assertEquals(0.2f, storedLatest.getPredictionConfidence());
        assertEquals("Manual direct backend test", storedLatest.getPredictionReasoning());
        assertTrue(storedLatest.getObservedAt().isAfter(earlier.getObservedAt()));
    }
}
