package com.neurolive.neuro_live_backend.presentation.dto;

import com.neurolive.neuro_live_backend.business.service.CrisisService;

import java.util.Map;

public record ClinicalAnalysisDTO(
        Long patientId,
        long totalEvents,
        long activeEvents,
        double averageDurationSeconds,
        double averageSamValence,
        double averageSamArousal,
        Map<String, Long> interventionCounts,
        Float baselineBpm,
        Float baselineSpo2
) {

    public static ClinicalAnalysisDTO from(CrisisService.ClinicalAnalysisSnapshot snapshot) {
        Map<String, Long> mappedCounts = snapshot.interventionCounts().entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        entry -> entry.getKey().name(),
                        Map.Entry::getValue
                ));

        return new ClinicalAnalysisDTO(
                snapshot.patientId(),
                snapshot.totalEvents(),
                snapshot.activeEvents(),
                snapshot.averageDurationSeconds(),
                snapshot.averageSamValence(),
                snapshot.averageSamArousal(),
                mappedCounts,
                snapshot.baselineBpm(),
                snapshot.baselineSpo2()
        );
    }
}
