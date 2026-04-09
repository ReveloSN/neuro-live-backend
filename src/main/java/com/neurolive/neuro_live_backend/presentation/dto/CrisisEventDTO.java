package com.neurolive.neuro_live_backend.presentation.dto;

import com.neurolive.neuro_live_backend.domain.crisis.CrisisEvent;

import java.time.LocalDateTime;

public record CrisisEventDTO(
        Long id,
        Long patientId,
        String state,
        String emotionalState,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        Long durationSeconds,
        String interventionType,
        Float triggerBpm,
        Float triggerSpo2,
        Float typingErrorRate,
        Float typingDwellTime,
        Float typingFlightTime,
        Integer typingErrorCount,
        Integer samValence,
        Integer samArousal
) {

    public static CrisisEventDTO from(CrisisEvent crisisEvent) {
        return new CrisisEventDTO(
                crisisEvent.getId(),
                crisisEvent.getPatientId(),
                crisisEvent.getState().name(),
                crisisEvent.getEmotionalState().state().name(),
                crisisEvent.getStartedAt(),
                crisisEvent.getEndedAt(),
                crisisEvent.calculateDuration().getSeconds(),
                crisisEvent.getInterventionType() == null ? null : crisisEvent.getInterventionType().name(),
                crisisEvent.getTriggerBpm(),
                crisisEvent.getTriggerSpo2(),
                crisisEvent.getTypingErrorRate(),
                crisisEvent.getTypingDwellTime(),
                crisisEvent.getTypingFlightTime(),
                crisisEvent.getTypingErrorCount(),
                crisisEvent.getSamValence(),
                crisisEvent.getSamArousal()
        );
    }
}
