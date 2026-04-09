package com.neurolive.neuro_live_backend.presentation.dto;

import com.neurolive.neuro_live_backend.domain.biometric.BaseLine;

import java.time.LocalDateTime;

public record BaselineResponseDTO(
        Long patientId,
        Float avgBpm,
        Float avgSpo2,
        LocalDateTime calculatedAt,
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
