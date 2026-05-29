package com.neurolive.neuro_live_backend.business.service;

import com.neurolive.neuro_live_backend.domain.biometric.BiometricTelemetrySample;
import com.neurolive.neuro_live_backend.repository.BiometricTelemetrySampleRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BiometricTelemetryQueryService {

    private final BiometricTelemetrySampleRepository biometricTelemetrySampleRepository;

    public BiometricTelemetryQueryService(BiometricTelemetrySampleRepository biometricTelemetrySampleRepository) {
        this.biometricTelemetrySampleRepository = biometricTelemetrySampleRepository;
    }

    public BiometricTelemetrySample findLatestForPatient(Long patientId) {
        if (patientId == null || patientId <= 0) {
            throw new IllegalArgumentException("Patient reference must be a positive identifier");
        }

        return biometricTelemetrySampleRepository.findFirstByPatientIdOrderByObservedAtDesc(patientId)
                .orElseThrow(() -> new EntityNotFoundException("Latest telemetry not found for patient " + patientId));
    }
}
