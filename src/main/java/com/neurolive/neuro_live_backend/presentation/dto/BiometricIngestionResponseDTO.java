package com.neurolive.neuro_live_backend.presentation.dto;

import com.neurolive.neuro_live_backend.business.service.TelemetryIngestionResult;

import java.time.LocalDateTime;

public record BiometricIngestionResponseDTO(
        Long sampleId,
        Long patientId,
        boolean baselineReady,
        String emotionalState,
        boolean crisisDetected,
        String interventionType,
        LocalDateTime observedAt
) {

    public static BiometricIngestionResponseDTO from(TelemetryIngestionResult result) {
        String emotionalState = result.crisisMediationResult() == null
                ? "NORMALIZING"
                : result.crisisMediationResult().emotionalState().state().name();
        String interventionType = result.crisisMediationResult() == null
                || result.crisisMediationResult().interventionProtocol() == null
                ? null
                : result.crisisMediationResult().interventionProtocol().getType().name();

        return new BiometricIngestionResponseDTO(
                result.storedSample().getId(),
                result.storedSample().getPatientId(),
                result.baseLine().isReady(),
                emotionalState,
                result.crisisMediationResult() != null && result.crisisMediationResult().crisisDetected(),
                interventionType,
                result.storedSample().getObservedAt()
        );
    }
}
