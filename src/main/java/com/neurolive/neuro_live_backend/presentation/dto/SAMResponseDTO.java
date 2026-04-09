package com.neurolive.neuro_live_backend.presentation.dto;

import com.neurolive.neuro_live_backend.domain.crisis.SAMResponse;

import java.time.LocalDateTime;

public record SAMResponseDTO(
        Long id,
        Long crisisEventId,
        Long patientId,
        Integer valence,
        Integer arousal,
        LocalDateTime recordedAt
) {

    public static SAMResponseDTO from(SAMResponse samResponse) {
        return new SAMResponseDTO(
                samResponse.getId(),
                samResponse.getCrisisEvent().getId(),
                samResponse.getPatientId(),
                samResponse.getValence(),
                samResponse.getArousal(),
                samResponse.getRecordedAt()
        );
    }
}
