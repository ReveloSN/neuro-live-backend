package com.neurolive.neuro_live_backend.business.service;

import com.neurolive.neuro_live_backend.business.patterns.CrisisMediator;
import com.neurolive.neuro_live_backend.domain.biometric.BaseLine;
import com.neurolive.neuro_live_backend.domain.biometric.BiometricTelemetrySample;
import com.neurolive.neuro_live_backend.domain.biometric.Device;

public record TelemetryIngestionResult(
        BiometricTelemetrySample storedSample,
        Device updatedDevice,
        BaseLine baseLine,
        CrisisMediator.CrisisMediationResult crisisMediationResult
) {

    public TelemetryIngestionResult {
        if (storedSample == null) {
            throw new IllegalArgumentException("Stored telemetry sample is required");
        }
        if (updatedDevice == null) {
            throw new IllegalArgumentException("Updated device is required");
        }
        if (baseLine == null) {
            throw new IllegalArgumentException("Baseline result is required");
        }
    }
}
