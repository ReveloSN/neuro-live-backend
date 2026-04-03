package com.neurolive.neuro_live_backend.business.patterns;

import com.neurolive.neuro_live_backend.domain.crisis.EmotionalState;

import java.time.LocalDateTime;

// Representa el cambio de estado listo para el dashboard
public record PatientStateUpdate(
        Long patientId,
        EmotionalState emotionalState,
        boolean crisisDetected,
        boolean interventionPrepared,
        LocalDateTime observedAt,
        PatientStateAudience audience,
        Boolean deviceConnected
) {

    public PatientStateUpdate {
        if (patientId == null || patientId <= 0) {
            throw new IllegalArgumentException("Patient reference must be a positive identifier");
        }
        if (interventionPrepared && !crisisDetected) {
            throw new IllegalArgumentException("Intervention cannot be prepared without a detected crisis");
        }
        if (observedAt == null) {
            throw new IllegalArgumentException("Observed time is required");
        }
        if (audience == null) {
            throw new IllegalArgumentException("Patient state audience is required");
        }
        if (emotionalState == null && deviceConnected == null) {
            throw new IllegalArgumentException(
                    "Patient state update must include an emotional state or a device connection status");
        }
    }

    public static PatientStateUpdate monitoring(Long patientId,
                                                EmotionalState emotionalState,
                                                boolean crisisDetected,
                                                boolean interventionPrepared,
                                                LocalDateTime observedAt) {
        return new PatientStateUpdate(
                patientId,
                emotionalState,
                crisisDetected,
                interventionPrepared,
                observedAt,
                PatientStateAudience.ALL,
                null
        );
    }

    public static PatientStateUpdate caregiverDisconnectAlert(Long patientId, LocalDateTime observedAt) {
        return new PatientStateUpdate(
                patientId,
                null,
                false,
                false,
                observedAt,
                PatientStateAudience.CAREGIVER_ONLY,
                Boolean.FALSE
        );
    }

    public boolean shouldNotifyCaregiver() {
        return audience == PatientStateAudience.ALL || audience == PatientStateAudience.CAREGIVER_ONLY;
    }

    public boolean shouldNotifyDoctor() {
        return audience == PatientStateAudience.ALL || audience == PatientStateAudience.DOCTOR_ONLY;
    }

    public boolean isDisconnectAlert() {
        return Boolean.FALSE.equals(deviceConnected);
    }
}
