package com.neurolive.neuro_live_backend.repository;

import com.neurolive.neuro_live_backend.NeuroLiveBackendApplication;
import com.neurolive.neuro_live_backend.domain.analysis.KeystrokeDynamics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = NeuroLiveBackendApplication.class)
@Transactional
// Verifica la persistencia de la dinamica de tecleo.
class KeystrokeDynamicsRepositoryTest {

    @Autowired
    private KeystrokeDynamicsRepository keystrokeDynamicsRepository;

    @Test
    void shouldPersistCorrectly() {
        KeystrokeDynamics earlierSignal = KeystrokeDynamics.capture(
                141L,
                90.0f,
                120.0f,
                LocalDateTime.of(2026, 4, 2, 11, 20)
        );
        KeystrokeDynamics laterSignal = KeystrokeDynamics.capture(
                141L,
                105.0f,
                160.0f,
                LocalDateTime.of(2026, 4, 2, 11, 25)
        );

        keystrokeDynamicsRepository.save(earlierSignal);
        keystrokeDynamicsRepository.save(laterSignal);

        List<KeystrokeDynamics> storedSignals = keystrokeDynamicsRepository.findAllByUserIdOrderByTimestampDesc(141L);

        assertEquals(2, storedSignals.size());
        assertEquals(laterSignal.getTimestamp(), storedSignals.getFirst().getTimestamp());
        assertNotNull(storedSignals.getFirst().getId());
    }
}
