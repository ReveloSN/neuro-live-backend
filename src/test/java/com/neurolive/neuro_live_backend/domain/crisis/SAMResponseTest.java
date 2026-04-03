package com.neurolive.neuro_live_backend.domain.crisis;

import com.neurolive.neuro_live_backend.data.enums.StateEnum;
import com.neurolive.neuro_live_backend.data.enums.TypeEnum;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SAMResponseTest {

    @Test
    void shouldCreateValidSamResponse() {
        CrisisEvent crisisEvent = closedCrisisEvent(31L);

        SAMResponse samResponse = SAMResponse.create(
                crisisEvent,
                7,
                4,
                LocalDateTime.of(2026, 4, 1, 12, 10)
        );

        assertEquals(31L, samResponse.getPatientId());
        assertEquals(7, samResponse.getValence());
        assertEquals(4, samResponse.getArousal());
        assertEquals(crisisEvent, samResponse.getCrisisEvent());
    }

    @Test
    void shouldRejectValenceBelowOne() {
        CrisisEvent crisisEvent = closedCrisisEvent(32L);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> SAMResponse.create(crisisEvent, 0, 5, LocalDateTime.of(2026, 4, 1, 12, 11))
        );

        assertEquals("SAM valence must be between 1 and 9", exception.getMessage());
    }

    @Test
    void shouldRejectValenceAboveNine() {
        CrisisEvent crisisEvent = closedCrisisEvent(33L);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> SAMResponse.create(crisisEvent, 10, 5, LocalDateTime.of(2026, 4, 1, 12, 12))
        );

        assertEquals("SAM valence must be between 1 and 9", exception.getMessage());
    }

    @Test
    void shouldRejectArousalBelowOne() {
        CrisisEvent crisisEvent = closedCrisisEvent(34L);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> SAMResponse.create(crisisEvent, 5, 0, LocalDateTime.of(2026, 4, 1, 12, 13))
        );

        assertEquals("SAM arousal must be between 1 and 9", exception.getMessage());
    }

    @Test
    void shouldRejectArousalAboveNine() {
        CrisisEvent crisisEvent = closedCrisisEvent(35L);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> SAMResponse.create(crisisEvent, 5, 10, LocalDateTime.of(2026, 4, 1, 12, 14))
        );

        assertEquals("SAM arousal must be between 1 and 9", exception.getMessage());
    }

    @Test
    void shouldSetRecordedAtAutomatically() {
        CrisisEvent crisisEvent = closedCrisisEvent(36L);

        SAMResponse samResponse = SAMResponse.create(crisisEvent, 6, 6, null);

        assertNotNull(samResponse.getRecordedAt());
    }

    private CrisisEvent closedCrisisEvent(Long patientId) {
        CrisisEvent crisisEvent = CrisisEvent.open(
                patientId,
                StateEnum.ACTIVE_CRISIS,
                LocalDateTime.of(2026, 4, 1, 12, 0)
        );
        crisisEvent.close(
                LocalDateTime.of(2026, 4, 1, 12, 5),
                StateEnum.NORMAL,
                TypeEnum.CALM_MODE
        );
        return crisisEvent;
    }
}
