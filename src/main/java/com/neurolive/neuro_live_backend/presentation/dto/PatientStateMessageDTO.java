package com.neurolive.neuro_live_backend.presentation.dto;

import com.neurolive.neuro_live_backend.business.patterns.PatientStateUpdate;

import java.time.Instant;

public record PatientStateMessageDTO(
        Long patientId,
        String emotionalState,
        String label,
        boolean crisisDetected,
        boolean interventionPrepared,
        Instant observedAt,
        Boolean deviceConnected,
        Boolean sensorContact,
        boolean disconnectAlert,
        boolean sensorContactAlert
) {

    // Traduce el evento interno a un payload compacto y estable para el dashboard.
    public static PatientStateMessageDTO from(PatientStateUpdate update) {
        String emotionalState = update.emotionalState() == null
                ? null
                : update.emotionalState().state().name();

        return new PatientStateMessageDTO(
                update.patientId(),
                emotionalState,
                resolveLabel(update, emotionalState),
                update.crisisDetected(),
                update.interventionPrepared(),
                update.observedAt(),
                update.deviceConnected(),
                update.sensorContact(),
                update.isDisconnectAlert(),
                update.isSensorContactAlert()
        );
    }

    private static String resolveLabel(PatientStateUpdate update, String emotionalState) {
        if (update.isDisconnectAlert()) return "Dispositivo desconectado";
        if (update.isSensorContactAlert()) return "Sin contacto de sensor";
        return switch (emotionalState == null ? "" : emotionalState) {
            case "NORMAL" -> "Normal";
            case "RISK_ELEVATED" -> "Atención";
            case "ACTIVE_CRISIS" -> "Crisis activa";
            default -> null;
        };
    }
}
