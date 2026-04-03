package com.neurolive.neuro_live_backend.domain.biometric;

import java.time.LocalDateTime;

// Representa una muestra puntual de signos biometricos del paciente.
public record BiometricData(
        float bpm,
        float spo2,
        LocalDateTime timestamp
) {

    public BiometricData {
        if (!Float.isFinite(bpm) || bpm < 0) {
            throw new IllegalArgumentException("BPM must be a finite non-negative value");
        }
        if (!Float.isFinite(spo2) || spo2 < 0) {
            throw new IllegalArgumentException("SpO2 must be a finite non-negative value");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("Biometric sample timestamp is required");
        }
    }
}
