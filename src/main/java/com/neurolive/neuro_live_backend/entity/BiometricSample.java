package com.neurolive.neuro_live_backend.entity;

import java.time.LocalDateTime;

public record BiometricSample(
        float bpm,
        float spo2,
        LocalDateTime timestamp
) {

    public BiometricSample {
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
