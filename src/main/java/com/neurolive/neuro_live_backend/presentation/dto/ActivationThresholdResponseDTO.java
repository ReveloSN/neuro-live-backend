package com.neurolive.neuro_live_backend.presentation.dto;

import com.neurolive.neuro_live_backend.domain.biometric.ActivationThreshold;

import java.time.LocalDateTime;

public record ActivationThresholdResponseDTO(
        Long id,
        Long patientId,
        Long personalUserId,
        Long definedByUserId,
        Float bpmMin,
        Float bpmMax,
        Float spo2Min,
        Float errorRateMax,
        Boolean active,
        LocalDateTime createdAt
) {

    public static ActivationThresholdResponseDTO from(ActivationThreshold threshold) {
        return new ActivationThresholdResponseDTO(
                threshold.getId(),
                threshold.getPatientId(),
                threshold.getPersonalUserId(),
                threshold.getDefinedByUserId(),
                threshold.getBpmMin(),
                threshold.getBpmMax(),
                threshold.getSpo2Min(),
                threshold.getErrorRateMax(),
                threshold.getActive(),
                threshold.getCreatedAt()
        );
    }
}
