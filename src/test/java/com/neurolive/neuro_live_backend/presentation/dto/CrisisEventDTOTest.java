package com.neurolive.neuro_live_backend.presentation.dto;

import com.neurolive.neuro_live_backend.data.enums.StateEnum;
import com.neurolive.neuro_live_backend.data.enums.TypeEnum;
import com.neurolive.neuro_live_backend.domain.crisis.CrisisEvent;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrisisEventDTOTest {

    @Test
    void shouldUseEndedAtToCalculateClosedCrisisDuration() {
        CrisisEvent crisisEvent = CrisisEvent.open(
                71L,
                StateEnum.ACTIVE_CRISIS,
                LocalDateTime.of(2026, 4, 2, 12, 0)
        );
        crisisEvent.close(
                LocalDateTime.of(2026, 4, 2, 12, 2, 30),
                StateEnum.NORMAL,
                TypeEnum.BREATHING
        );

        CrisisEventDTO dto = CrisisEventDTO.from(crisisEvent);

        assertEquals(LocalDateTime.of(2026, 4, 2, 12, 2, 30), dto.endedAt());
        assertEquals(150L, dto.durationSeconds());
        assertEquals("NORMAL", dto.state());
    }

    @Test
    void shouldExposeOpenCrisisAsActiveInsteadOfClosedHistoryEvent() {
        CrisisEvent crisisEvent = CrisisEvent.open(
                72L,
                StateEnum.ACTIVE_CRISIS,
                LocalDateTime.now().minusSeconds(5)
        );

        CrisisEventDTO dto = CrisisEventDTO.from(crisisEvent);

        assertNull(dto.endedAt());
        assertEquals("ACTIVE_CRISIS", dto.state());
        assertTrue(dto.durationSeconds() >= 0);
    }
}
