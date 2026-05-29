package com.neurolive.neuro_live_backend.business.service;

import com.neurolive.neuro_live_backend.business.patterns.CrisisMediator;
import com.neurolive.neuro_live_backend.data.enums.StateEnum;
import com.neurolive.neuro_live_backend.data.enums.TypeEnum;
import com.neurolive.neuro_live_backend.domain.crisis.CrisisEvent;
import com.neurolive.neuro_live_backend.domain.crisis.InterventionProtocol;
import com.neurolive.neuro_live_backend.repository.CrisisEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
// Evita mezclar evaluacion con persistencia
public class CrisisOutcomePersistenceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CrisisOutcomePersistenceService.class);

    private final CrisisEventRepository crisisEventRepository;
    private final ClinicalInsightService clinicalInsightService;

    public CrisisOutcomePersistenceService(CrisisEventRepository crisisEventRepository,
                                           ClinicalInsightService clinicalInsightService) {
        this.crisisEventRepository = crisisEventRepository;
        this.clinicalInsightService = clinicalInsightService;
    }

    public Optional<CrisisEvent> persist(CrisisMediator.CrisisMediationResult crisisMediationResult) {
        if (crisisMediationResult == null) {
            throw new IllegalArgumentException("Crisis mediation result is required");
        }
        if (!crisisMediationResult.crisisDetected()) {
            return Optional.empty();
        }

        CrisisEvent crisisEvent = resolveTargetEvent(crisisMediationResult);
        attachPreparedProtocolIfNeeded(crisisEvent, crisisMediationResult.interventionProtocol());

        // Persiste la crisis solo cuando el mediador la confirma
        return Optional.of(crisisEventRepository.save(crisisEvent));
    }

    public List<CrisisEvent> closeActiveCrisesIfRecovered(Long patientId,
                                                          StateEnum finalState,
                                                          LocalDateTime observedAt) {
        Long validatedPatientId = validatePatientId(patientId);
        StateEnum validatedFinalState = validateRecoveredState(finalState);
        if (validatedFinalState == StateEnum.ACTIVE_CRISIS) {
            return List.of();
        }

        List<CrisisEvent> activeCrises = crisisEventRepository
                .findAllByPatientIdAndEndedAtIsNullOrderByStartedAtDesc(validatedPatientId);
        if (activeCrises.isEmpty()) {
            LOGGER.debug(
                    "No active crisis to close patientId={} finalState={} observedAt={}",
                    validatedPatientId,
                    validatedFinalState,
                    observedAt
            );
            return List.of();
        }

        return activeCrises.stream()
                .map(crisisEvent -> closeActiveCrisis(crisisEvent, validatedFinalState, observedAt))
                .toList();
    }

    private CrisisEvent closeActiveCrisis(CrisisEvent crisisEvent,
                                          StateEnum finalState,
                                          LocalDateTime observedAt) {
        LocalDateTime endedAt = resolveSafeEndedAt(crisisEvent, observedAt);
        TypeEnum interventionType = resolveInterventionType(crisisEvent);
        LOGGER.info(
                "Closing active crisis id={} patientId={} endedAt={} finalState={} interventionType={}",
                crisisEvent.getId(),
                crisisEvent.getPatientId(),
                endedAt,
                finalState,
                interventionType
        );

        crisisEvent.close(endedAt, finalState, interventionType);
        CrisisEvent savedEvent = crisisEventRepository.save(crisisEvent);
        clinicalInsightService.generatePostCrisisSummaryAsync(savedEvent.getId());
        return savedEvent;
    }

    private CrisisEvent resolveTargetEvent(CrisisMediator.CrisisMediationResult crisisMediationResult) {
        CrisisEvent candidateEvent = requireCrisisEvent(crisisMediationResult);

        Optional<CrisisEvent> activeCrisis = crisisEventRepository
                .findFirstByPatientIdAndEndedAtIsNullOrderByStartedAtDesc(candidateEvent.getPatientId());
        if (activeCrisis.isPresent()) {
            CrisisEvent existingCrisis = activeCrisis.orElseThrow();
            LOGGER.debug(
                    "Reusing active crisis id={} patientId={} instead of creating duplicate",
                    existingCrisis.getId(),
                    existingCrisis.getPatientId()
            );
            return existingCrisis;
        }

        LOGGER.info(
                "Opening crisis for patientId={} state={} observedAt={}",
                candidateEvent.getPatientId(),
                candidateEvent.getState(),
                candidateEvent.getStartedAt()
        );
        return candidateEvent;
    }

    private CrisisEvent requireCrisisEvent(CrisisMediator.CrisisMediationResult crisisMediationResult) {
        CrisisEvent crisisEvent = crisisMediationResult.crisisEvent();
        if (crisisEvent == null) {
            throw new IllegalArgumentException("Crisis event is required when a crisis is detected");
        }
        return crisisEvent;
    }

    // Deja el evento abierto para el cierre posterior con SAM
    private void attachPreparedProtocolIfNeeded(CrisisEvent crisisEvent, InterventionProtocol interventionProtocol) {
        if (interventionProtocol == null || crisisEvent.getInterventionProtocol() != null) {
            return;
        }

        crisisEvent.attachInterventionProtocol(interventionProtocol);
    }

    private Long validatePatientId(Long patientId) {
        if (patientId == null || patientId <= 0) {
            throw new IllegalArgumentException("Patient reference must be a positive identifier");
        }
        return patientId;
    }

    private StateEnum validateRecoveredState(StateEnum finalState) {
        if (finalState == null) {
            throw new IllegalArgumentException("Final crisis state is required");
        }
        return finalState;
    }

    // Cierra la crisis con una marca temporal segura aunque la muestra llegue desordenada.
    private LocalDateTime resolveSafeEndedAt(CrisisEvent crisisEvent, LocalDateTime observedAt) {
        LocalDateTime candidateEndedAt = observedAt == null ? LocalDateTime.now() : observedAt;
        if (!candidateEndedAt.isBefore(crisisEvent.getStartedAt())) {
            return candidateEndedAt;
        }

        LOGGER.warn(
                "Recovery timestamp before crisis start crisisId={} patientId={} observedAt={} startedAt={}",
                crisisEvent.getId(),
                crisisEvent.getPatientId(),
                candidateEndedAt,
                crisisEvent.getStartedAt()
        );
        return crisisEvent.getStartedAt();
    }

    private TypeEnum resolveInterventionType(CrisisEvent crisisEvent) {
        if (crisisEvent.getInterventionType() != null) {
            return crisisEvent.getInterventionType().canonical();
        }
        if (crisisEvent.getInterventionProtocol() != null) {
            return crisisEvent.getInterventionProtocol().getType().canonical();
        }
        return TypeEnum.NO_INTERVENTION;
    }
}
