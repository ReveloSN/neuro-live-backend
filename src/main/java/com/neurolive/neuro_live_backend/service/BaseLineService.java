package com.neurolive.neuro_live_backend.service;

import com.neurolive.neuro_live_backend.entity.BaseLine;
import com.neurolive.neuro_live_backend.entity.BiometricSample;
import com.neurolive.neuro_live_backend.entity.User;
import com.neurolive.neuro_live_backend.enums.RoleName;
import com.neurolive.neuro_live_backend.repository.BaseLineRepository;
import com.neurolive.neuro_live_backend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

@Service
@Transactional
public class BaseLineService {

    private final BaseLineRepository baseLineRepository;
    private final UserRepository userRepository;

    public BaseLineService(BaseLineRepository baseLineRepository,
                           UserRepository userRepository) {
        this.baseLineRepository = baseLineRepository;
        this.userRepository = userRepository;
    }

    public BaseLine calculate(Long patientId, Collection<BiometricSample> biometricSamples) {
        validatePatientReference(patientId);

        BaseLine baseLine = baseLineRepository.findByPatientId(patientId)
                .orElseGet(() -> new BaseLine(patientId));

        baseLine.calculate(biometricSamples);
        return baseLineRepository.save(baseLine);
    }

    public BaseLine updateFromTelemetry(Long patientId, Collection<BiometricSample> biometricSamples) {
        return calculate(patientId, biometricSamples);
    }

    @Transactional(readOnly = true)
    public BaseLine findByPatientId(Long patientId) {
        validatePatientReference(patientId);

        return baseLineRepository.findByPatientId(patientId)
                .orElseThrow(() -> new EntityNotFoundException("Baseline not found for patient " + patientId));
    }

    private User validatePatientReference(Long patientId) {
        if (patientId == null || patientId <= 0) {
            throw new IllegalArgumentException("Patient reference must be a positive identifier");
        }

        User patient = userRepository.findById(patientId)
                .orElseThrow(() -> new EntityNotFoundException("Patient not found with id " + patientId));

        if (patient.getRole() == null || patient.getRole().getName() != RoleName.PACIENTE) {
            throw new IllegalArgumentException("Referenced user is not a patient");
        }

        return patient;
    }
}
