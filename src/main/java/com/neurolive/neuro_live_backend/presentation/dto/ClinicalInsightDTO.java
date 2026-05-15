package com.neurolive.neuro_live_backend.presentation.dto;

import java.time.LocalDateTime;

// Expone una narrativa clinica segura para dashboards medicos.
public record ClinicalInsightDTO(
                Long patientId,
                LocalDateTime start,
                LocalDateTime end,
                String insight) {
}
