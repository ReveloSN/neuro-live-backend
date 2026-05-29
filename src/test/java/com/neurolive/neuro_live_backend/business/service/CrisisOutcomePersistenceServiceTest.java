package com.neurolive.neuro_live_backend.business.service;

import com.neurolive.neuro_live_backend.business.patterns.CrisisMediator;
import com.neurolive.neuro_live_backend.data.enums.StateEnum;
import com.neurolive.neuro_live_backend.data.enums.TypeEnum;
import com.neurolive.neuro_live_backend.domain.crisis.CrisisEvent;
import com.neurolive.neuro_live_backend.domain.crisis.EmotionalState;
import com.neurolive.neuro_live_backend.domain.crisis.InterventionProtocol;
import com.neurolive.neuro_live_backend.repository.CrisisEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CrisisOutcomePersistenceServiceTest {

    @Mock
    private CrisisEventRepository crisisEventRepository;

    @Mock
    private ClinicalInsightService clinicalInsightService;

    @InjectMocks
    private CrisisOutcomePersistenceService crisisOutcomePersistenceService;

    @Test
    void shouldNotPersistCrisisEventWhenStateIsNormal() {
        Optional<CrisisEvent> persistedOutcome = crisisOutcomePersistenceService.persist(
                CrisisMediator.CrisisMediationResult.withoutCrisis(
                        EmotionalState.from(StateEnum.NORMAL)
                )
        );

        assertTrue(persistedOutcome.isEmpty());
        verify(crisisEventRepository, never()).save(any());
    }

    @Test
    void shouldNotPersistCrisisEventWhenStateIsAtRisk() {
        Optional<CrisisEvent> persistedOutcome = crisisOutcomePersistenceService.persist(
                CrisisMediator.CrisisMediationResult.withoutCrisis(
                        EmotionalState.from(StateEnum.RISK_ELEVATED)
                )
        );

        assertTrue(persistedOutcome.isEmpty());
        verify(crisisEventRepository, never()).save(any());
    }

    @Test
    void shouldPersistCrisisEventWhenStateIsCrisis() {
        CrisisMediator.CrisisMediationResult mediationResult = buildDetectedCrisisResult(101L);
        when(crisisEventRepository.findFirstByPatientIdAndEndedAtIsNullOrderByStartedAtDesc(101L))
                .thenReturn(Optional.empty());
        when(crisisEventRepository.save(any(CrisisEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Optional<CrisisEvent> persistedOutcome = crisisOutcomePersistenceService.persist(mediationResult);

        assertTrue(persistedOutcome.isPresent());
        assertEquals(101L, persistedOutcome.get().getPatientId());
        assertTrue(persistedOutcome.get().isActive());
        verify(crisisEventRepository).save(any(CrisisEvent.class));
    }

    @Test
    void shouldPersistInterventionProtocolWhenCrisisProtocolExists() {
        CrisisMediator.CrisisMediationResult mediationResult = buildDetectedCrisisResult(102L);
        when(crisisEventRepository.findFirstByPatientIdAndEndedAtIsNullOrderByStartedAtDesc(102L))
                .thenReturn(Optional.empty());
        when(crisisEventRepository.save(any(CrisisEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        crisisOutcomePersistenceService.persist(mediationResult);

        ArgumentCaptor<CrisisEvent> eventCaptor = ArgumentCaptor.forClass(CrisisEvent.class);
        verify(crisisEventRepository).save(eventCaptor.capture());
        assertNotNull(eventCaptor.getValue().getInterventionProtocol());
        assertEquals(TypeEnum.BREATHING, eventCaptor.getValue().getInterventionType());
    }

    @Test
    void shouldLinkCrisisEventAndInterventionProtocolCorrectly() {
        CrisisMediator.CrisisMediationResult mediationResult = buildDetectedCrisisResult(103L);
        when(crisisEventRepository.findFirstByPatientIdAndEndedAtIsNullOrderByStartedAtDesc(103L))
                .thenReturn(Optional.empty());
        when(crisisEventRepository.save(any(CrisisEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CrisisEvent persistedEvent = crisisOutcomePersistenceService.persist(mediationResult).orElseThrow();

        assertSame(persistedEvent, persistedEvent.getInterventionProtocol().getCrisisEvent());
        assertEquals(TypeEnum.BREATHING, persistedEvent.getInterventionProtocol().getType());
        assertTrue(persistedEvent.isActive());
    }

    @Test
    void shouldReuseExistingOpenCrisisEventInsteadOfCreatingDuplicate() {
        CrisisEvent existingOpenEvent = CrisisEvent.open(
                104L,
                StateEnum.ACTIVE_CRISIS,
                LocalDateTime.of(2026, 4, 2, 12, 0)
        );
        CrisisMediator.CrisisMediationResult mediationResult = buildDetectedCrisisResult(104L);
        when(crisisEventRepository.findFirstByPatientIdAndEndedAtIsNullOrderByStartedAtDesc(104L))
                .thenReturn(Optional.of(existingOpenEvent));
        when(crisisEventRepository.save(any(CrisisEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CrisisEvent persistedEvent = crisisOutcomePersistenceService.persist(mediationResult).orElseThrow();

        assertSame(existingOpenEvent, persistedEvent);
        assertNotNull(persistedEvent.getInterventionProtocol());
        assertFalse(persistedEvent.getInterventionProtocol().getActive());
    }

    @Test
    void shouldThrowWhenMediationResultIsNull() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> crisisOutcomePersistenceService.persist(null)
        );

        assertEquals("Crisis mediation result is required", exception.getMessage());
    }

    @Test
    void shouldKeepExistingProtocolWhenCrisisEventAlreadyHasOne() {
        InterventionProtocol existingProtocol = InterventionProtocol.builder(TypeEnum.LIGHT)
                .light("blue", 50)
                .build();
        CrisisEvent existingOpenEvent = CrisisEvent.open(
                105L,
                StateEnum.ACTIVE_CRISIS,
                LocalDateTime.of(2026, 4, 2, 12, 0)
        );
        existingOpenEvent.attachInterventionProtocol(existingProtocol);

        CrisisMediator.CrisisMediationResult mediationResult = buildDetectedCrisisResult(105L);
        when(crisisEventRepository.findFirstByPatientIdAndEndedAtIsNullOrderByStartedAtDesc(105L))
                .thenReturn(Optional.of(existingOpenEvent));
        when(crisisEventRepository.save(any(CrisisEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CrisisEvent result = crisisOutcomePersistenceService.persist(mediationResult).orElseThrow();

        assertEquals(TypeEnum.LIGHT, result.getInterventionType());
        assertSame(existingProtocol, result.getInterventionProtocol());
    }

    @Test
    void shouldCloseExistingOpenCrisisWhenPatientReturnsToNormal() {
        CrisisEvent existingOpenEvent = CrisisEvent.open(
                106L,
                StateEnum.ACTIVE_CRISIS,
                LocalDateTime.of(2026, 4, 2, 12, 0)
        );
        existingOpenEvent.attachInterventionProtocol(
                InterventionProtocol.builder(TypeEnum.BREATHING)
                        .breathingPattern(4, 6)
                        .active()
                        .build()
        );
        when(crisisEventRepository.findAllByPatientIdAndEndedAtIsNullOrderByStartedAtDesc(106L))
                .thenReturn(List.of(existingOpenEvent));
        when(crisisEventRepository.save(any(CrisisEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CrisisEvent closedEvent = crisisOutcomePersistenceService.closeActiveCrisesIfRecovered(
                106L,
                StateEnum.NORMAL,
                LocalDateTime.of(2026, 4, 2, 12, 5)
        ).getFirst();

        assertFalse(closedEvent.isActive());
        assertEquals(StateEnum.NORMAL, closedEvent.getState());
        assertEquals(LocalDateTime.of(2026, 4, 2, 12, 5), closedEvent.getEndedAt());
        assertEquals(TypeEnum.BREATHING, closedEvent.getInterventionType());
        verify(clinicalInsightService).generatePostCrisisSummaryAsync(closedEvent.getId());
    }

    @Test
    void shouldCloseExistingOpenCrisisWhenPatientDropsToRiskElevated() {
        CrisisEvent existingOpenEvent = CrisisEvent.open(
                107L,
                StateEnum.ACTIVE_CRISIS,
                LocalDateTime.of(2026, 4, 2, 12, 0)
        );
        existingOpenEvent.attachInterventionProtocol(
                InterventionProtocol.builder(TypeEnum.LIGHT)
                        .light("blue", 40)
                        .active()
                        .build()
        );
        when(crisisEventRepository.findAllByPatientIdAndEndedAtIsNullOrderByStartedAtDesc(107L))
                .thenReturn(List.of(existingOpenEvent));
        when(crisisEventRepository.save(any(CrisisEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CrisisEvent closedEvent = crisisOutcomePersistenceService.closeActiveCrisesIfRecovered(
                107L,
                StateEnum.RISK_ELEVATED,
                LocalDateTime.of(2026, 4, 2, 12, 3)
        ).getFirst();

        assertFalse(closedEvent.isActive());
        assertEquals(StateEnum.RISK_ELEVATED, closedEvent.getState());
        assertNotNull(closedEvent.getEndedAt());
        assertEquals(TypeEnum.LIGHT, closedEvent.getInterventionType());
    }

    @Test
    void shouldNotCreateCrisisWhenRecoveredStateArrivesWithoutOpenCrisis() {
        when(crisisEventRepository.findAllByPatientIdAndEndedAtIsNullOrderByStartedAtDesc(108L))
                .thenReturn(List.of());

        List<CrisisEvent> closedEvents = crisisOutcomePersistenceService.closeActiveCrisesIfRecovered(
                108L,
                StateEnum.NORMAL,
                LocalDateTime.of(2026, 4, 2, 12, 5)
        );

        assertTrue(closedEvents.isEmpty());
        verify(crisisEventRepository, never()).save(any());
        verify(clinicalInsightService, never()).generatePostCrisisSummaryAsync(any());
    }

    @Test
    void shouldNotCloseCrisisWhenStateRemainsActiveCrisis() {
        List<CrisisEvent> closedEvents = crisisOutcomePersistenceService.closeActiveCrisesIfRecovered(
                109L,
                StateEnum.ACTIVE_CRISIS,
                LocalDateTime.of(2026, 4, 2, 12, 5)
        );

        assertTrue(closedEvents.isEmpty());
        verify(crisisEventRepository, never()).findAllByPatientIdAndEndedAtIsNullOrderByStartedAtDesc(any());
        verify(crisisEventRepository, never()).save(any());
    }

    @Test
    void shouldUseCrisisStartWhenRecoveryTimestampIsBeforeStart() {
        CrisisEvent existingOpenEvent = CrisisEvent.open(
                110L,
                StateEnum.ACTIVE_CRISIS,
                LocalDateTime.of(2026, 4, 2, 12, 10)
        );
        existingOpenEvent.attachInterventionProtocol(
                InterventionProtocol.builder(TypeEnum.BREATHING)
                        .breathingPattern(4, 6)
                        .active()
                        .build()
        );
        when(crisisEventRepository.findAllByPatientIdAndEndedAtIsNullOrderByStartedAtDesc(110L))
                .thenReturn(List.of(existingOpenEvent));
        when(crisisEventRepository.save(any(CrisisEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CrisisEvent closedEvent = crisisOutcomePersistenceService.closeActiveCrisesIfRecovered(
                110L,
                StateEnum.NORMAL,
                LocalDateTime.of(2026, 4, 2, 12, 5)
        ).getFirst();

        assertEquals(existingOpenEvent.getStartedAt(), closedEvent.getEndedAt());
        assertFalse(closedEvent.calculateDuration().isNegative());
    }

    @Test
    void shouldCloseEveryExistingOpenCrisisForRecoveredPatient() {
        CrisisEvent newestOpenEvent = CrisisEvent.open(
                111L,
                StateEnum.ACTIVE_CRISIS,
                LocalDateTime.of(2026, 4, 2, 12, 10)
        );
        newestOpenEvent.attachInterventionProtocol(
                InterventionProtocol.builder(TypeEnum.BREATHING)
                        .breathingPattern(4, 6)
                        .active()
                        .build()
        );
        CrisisEvent olderOpenEvent = CrisisEvent.open(
                111L,
                StateEnum.ACTIVE_CRISIS,
                LocalDateTime.of(2026, 4, 2, 12, 0)
        );
        olderOpenEvent.attachInterventionProtocol(
                InterventionProtocol.builder(TypeEnum.AUDIO)
                        .audioTrack("calm.mp3", 30)
                        .active()
                        .build()
        );
        when(crisisEventRepository.findAllByPatientIdAndEndedAtIsNullOrderByStartedAtDesc(111L))
                .thenReturn(List.of(newestOpenEvent, olderOpenEvent));
        when(crisisEventRepository.save(any(CrisisEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<CrisisEvent> closedEvents = crisisOutcomePersistenceService.closeActiveCrisesIfRecovered(
                111L,
                StateEnum.NORMAL,
                LocalDateTime.of(2026, 4, 2, 12, 15)
        );

        assertEquals(2, closedEvents.size());
        assertFalse(newestOpenEvent.isActive());
        assertFalse(olderOpenEvent.isActive());
        verify(crisisEventRepository, org.mockito.Mockito.times(2)).save(any(CrisisEvent.class));
        verify(clinicalInsightService, org.mockito.Mockito.times(2)).generatePostCrisisSummaryAsync(any());
    }

    private CrisisMediator.CrisisMediationResult buildDetectedCrisisResult(Long patientId) {
        CrisisEvent crisisEvent = CrisisEvent.open(
                patientId,
                StateEnum.ACTIVE_CRISIS,
                LocalDateTime.of(2026, 4, 2, 12, 5)
        );
        InterventionProtocol interventionProtocol = InterventionProtocol.builder(TypeEnum.BREATHING)
                .breathingPattern(4, 6)
                .build();

        return CrisisMediator.CrisisMediationResult.crisisDetected(
                EmotionalState.from(StateEnum.ACTIVE_CRISIS),
                crisisEvent,
                interventionProtocol
        );
    }
}
