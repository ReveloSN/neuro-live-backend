package com.neurolive.neuro_live_backend.presentation.dto;

import com.neurolive.neuro_live_backend.business.patterns.PatientStateUpdate;

import java.time.LocalDateTime;

public record PatientStateMessageDTO(
        Long patientId,
        String emotionalState,
        boolean crisisDetected,
        boolean interventionPrepared,
        LocalDateTime observedAt,
        Boolean deviceConnected,
        boolean disconnectAlert
) {

    public static PatientStateMessageDTO from(PatientStateUpdate update) {
        String emotionalState = update.emotionalState() == null
                ? null
                : update.emotionalState().state().name();

        return new PatientStateMessageDTO(
                update.patientId(),
                emotionalState,
                update.crisisDetected(),
                update.interventionPrepared(),
                update.observedAt(),
                update.deviceConnected(),
                update.isDisconnectAlert()
        );
    }
}
