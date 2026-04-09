package com.neurolive.neuro_live_backend.domain.crisis;

import com.neurolive.neuro_live_backend.data.enums.StateEnum;
import com.neurolive.neuro_live_backend.data.enums.TypeEnum;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Valida el ciclo de vida y las reglas de un evento de crisis.
class CrisisEventTest {

    @Test
    void shouldCreateCrisisEventCorrectly() {
        LocalDateTime startedAt = LocalDateTime.of(2026, 4, 1, 10, 0);

        CrisisEvent crisisEvent = CrisisEvent.open(12L, StateEnum.ACTIVE_CRISIS, startedAt);

        assertEquals(12L, crisisEvent.getPatientId());
        assertEquals(startedAt, crisisEvent.getStartedAt());
        assertEquals(StateEnum.ACTIVE_CRISIS, crisisEvent.getState());
        assertTrue(crisisEvent.isActive());
    }

    @Test
    void shouldRejectInvalidClosingTime() {
        CrisisEvent crisisEvent = CrisisEvent.open(
                14L,
                StateEnum.RISK_ELEVATED,
                LocalDateTime.of(2026, 4, 1, 10, 0)
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> crisisEvent.close(
                        LocalDateTime.of(2026, 4, 1, 9, 59),
                        StateEnum.NORMAL,
                        TypeEnum.CALM_MODE
                )
        );

        assertEquals("Crisis event cannot end before it starts", exception.getMessage());
    }

    @Test
    void shouldCalculateDurationCorrectly() {
        CrisisEvent crisisEvent = CrisisEvent.open(
                18L,
                StateEnum.ACTIVE_CRISIS,
                LocalDateTime.of(2026, 4, 1, 10, 0)
        );

        crisisEvent.close(
                LocalDateTime.of(2026, 4, 1, 10, 7, 30),
                StateEnum.NORMAL,
                TypeEnum.BREATHING
        );

        assertEquals(Duration.ofMinutes(7).plusSeconds(30), crisisEvent.calculateDuration());
    }

    @Test
    void shouldAttachSamResponseToCrisisEventCorrectly() {
        CrisisEvent crisisEvent = CrisisEvent.open(
                22L,
                StateEnum.ACTIVE_CRISIS,
                LocalDateTime.of(2026, 4, 1, 11, 0)
        );

        crisisEvent.close(
                LocalDateTime.of(2026, 4, 1, 11, 5),
                StateEnum.RISK_ELEVATED,
                TypeEnum.AUDIO
        );
        crisisEvent.attachSamData(7, 4);

        assertEquals(7, crisisEvent.getSamValence());
        assertEquals(4, crisisEvent.getSamArousal());
        assertEquals(22L, crisisEvent.getSamResponse().getPatientId());
        assertEquals(crisisEvent, crisisEvent.getSamResponse().getCrisisEvent());
    }

    @Test
    void shouldRejectInvalidSamValues() {
        CrisisEvent crisisEvent = CrisisEvent.open(
                24L,
                StateEnum.ACTIVE_CRISIS,
                LocalDateTime.of(2026, 4, 1, 11, 30)
        );

        crisisEvent.close(
                LocalDateTime.of(2026, 4, 1, 11, 35),
                StateEnum.NORMAL,
                TypeEnum.LIGHT
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> crisisEvent.attachSamData(10, 3)
        );

        assertEquals("SAM valence must be between 1 and 9", exception.getMessage());
    }

    @Test
    void shouldKeepCrisisEventConsistentAfterAttachingSamResponse() {
        CrisisEvent crisisEvent = CrisisEvent.open(
                26L,
                StateEnum.ACTIVE_CRISIS,
                LocalDateTime.of(2026, 4, 1, 11, 40)
        );

        crisisEvent.close(
                LocalDateTime.of(2026, 4, 1, 11, 50),
                StateEnum.NORMAL,
                TypeEnum.CALM_MODE
        );
        crisisEvent.attachSamResponse(
                SAMResponse.create(
                        crisisEvent,
                        6,
                        5,
                        LocalDateTime.of(2026, 4, 1, 11, 52)
                )
        );

        assertFalse(crisisEvent.isActive());
        assertEquals(TypeEnum.CALM_MODE, crisisEvent.getInterventionType());
        assertEquals(6, crisisEvent.getSamValence());
        assertEquals(5, crisisEvent.getSamArousal());
    }

    @Test
    void shouldAttachProtocolToCrisisEventCorrectly() {
        CrisisEvent crisisEvent = CrisisEvent.open(
                28L,
                StateEnum.ACTIVE_CRISIS,
                LocalDateTime.of(2026, 4, 1, 11, 55)
        );

        InterventionProtocol interventionProtocol = InterventionProtocol.builder(TypeEnum.BREATHING)
                .breathingPattern(4, 6)
                .build();

        crisisEvent.attachInterventionProtocol(interventionProtocol);

        assertEquals(interventionProtocol, crisisEvent.getInterventionProtocol());
        assertEquals(TypeEnum.BREATHING, crisisEvent.getInterventionProtocol().getType());
        assertTrue(crisisEvent.isActive());
    }

    @Test
    void shouldKeepCrisisEventConsistentAfterAttachingProtocol() {
        CrisisEvent crisisEvent = CrisisEvent.open(
                29L,
                StateEnum.ACTIVE_CRISIS,
                LocalDateTime.of(2026, 4, 1, 12, 0)
        );

        crisisEvent.close(
                LocalDateTime.of(2026, 4, 1, 12, 8),
                StateEnum.RISK_ELEVATED,
                TypeEnum.LIGHT
        );

        crisisEvent.attachInterventionProtocol(
                InterventionProtocol.builder(TypeEnum.LIGHT)
                        .light("blue", 55)
                        .active()
                        .build()
        );

        assertFalse(crisisEvent.isActive());
        assertEquals(TypeEnum.LIGHT, crisisEvent.getInterventionType());
        assertTrue(crisisEvent.getInterventionProtocol().getActive());
    }

    @Test
    void shouldCloseCrisisEventAfterPreparedProtocolIsAttached() {
        CrisisEvent crisisEvent = CrisisEvent.open(
                30L,
                StateEnum.ACTIVE_CRISIS,
                LocalDateTime.of(2026, 4, 1, 12, 12)
        );

        crisisEvent.attachInterventionProtocol(
                InterventionProtocol.builder(TypeEnum.AUDIO)
                        .audioTrack("calm-track.mp3", 20)
                        .build()
        );

        crisisEvent.close(
                LocalDateTime.of(2026, 4, 1, 12, 20),
                StateEnum.RISK_ELEVATED,
                TypeEnum.AUDIO
        );

        assertFalse(crisisEvent.isActive());
        assertEquals(TypeEnum.AUDIO, crisisEvent.getInterventionType());
        assertNotNull(crisisEvent.getInterventionProtocol());
    }

    @Test
    void shouldReportActiveAndInactiveStateCorrectly() {
        CrisisEvent crisisEvent = CrisisEvent.open(
                30L,
                StateEnum.ACTIVE_CRISIS,
                LocalDateTime.of(2026, 4, 1, 12, 0)
        );

        assertTrue(crisisEvent.isActive());

        crisisEvent.close(
                LocalDateTime.of(2026, 4, 1, 12, 10),
                StateEnum.NORMAL,
                TypeEnum.NO_INTERVENTION
        );

        assertFalse(crisisEvent.isActive());
    }

    @Test
    void shouldExposeEmotionalStateFromCrisisEvent() {
        CrisisEvent crisisEvent = CrisisEvent.open(
                31L,
                StateEnum.ACTIVE_CRISIS,
                LocalDateTime.of(2026, 4, 1, 12, 15)
        );

        assertTrue(crisisEvent.getEmotionalState().isCrisis());
        assertEquals("red", crisisEvent.getEmotionalState().colorKey());

        crisisEvent.close(
                LocalDateTime.of(2026, 4, 1, 12, 20),
                StateEnum.RISK_ELEVATED,
                TypeEnum.NO_INTERVENTION
        );

        assertTrue(crisisEvent.getEmotionalState().isAtRisk());
        assertEquals("yellow", crisisEvent.getEmotionalState().colorKey());
    }
}
