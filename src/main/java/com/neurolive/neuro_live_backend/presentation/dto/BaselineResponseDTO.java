package com.neurolive.neuro_live_backend.presentation.dto;

import com.neurolive.neuro_live_backend.domain.biometric.BaseLine;

import java.time.Instant;

public record BaselineResponseDTO(
        Long patientId,
        Float avgBpm,
        Float avgSpo2,
        Instant calculatedAt,
        boolean ready
) {

    public static BaselineResponseDTO from(BaseLine baseLine) {
        return new BaselineResponseDTO(
                baseLine.getPatientId(),
                baseLine.getAvgBpm(),
                baseLine.getAvgSpo2(),
                baseLine.getCalculatedAt(),
                baseLine.isReady()
        );
    }
}
