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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CrisisOutcomePersistenceServiceTest {

    @Mock
    private CrisisEventRepository crisisEventRepository;

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
