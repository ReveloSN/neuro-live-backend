package com.neurolive.neuro_live_backend.business.service;

import com.neurolive.neuro_live_backend.business.patterns.CrisisMediator;
import com.neurolive.neuro_live_backend.domain.crisis.CrisisEvent;
import com.neurolive.neuro_live_backend.domain.crisis.InterventionProtocol;
import com.neurolive.neuro_live_backend.repository.CrisisEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
// Evita mezclar evaluacion con persistencia
public class CrisisOutcomePersistenceService {

    private final CrisisEventRepository crisisEventRepository;

    public CrisisOutcomePersistenceService(CrisisEventRepository crisisEventRepository) {
        this.crisisEventRepository = crisisEventRepository;
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

    private CrisisEvent resolveTargetEvent(CrisisMediator.CrisisMediationResult crisisMediationResult) {
        CrisisEvent candidateEvent = requireCrisisEvent(crisisMediationResult);

        return crisisEventRepository.findFirstByPatientIdAndEndedAtIsNullOrderByStartedAtDesc(candidateEvent.getPatientId())
                .orElse(candidateEvent);
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
}
