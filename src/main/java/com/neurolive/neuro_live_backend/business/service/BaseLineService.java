package com.neurolive.neuro_live_backend.business.service;

import com.neurolive.neuro_live_backend.domain.biometric.BaseLine;
import com.neurolive.neuro_live_backend.domain.biometric.BiometricData;
import com.neurolive.neuro_live_backend.domain.user.Patient;
import com.neurolive.neuro_live_backend.domain.user.User;
import com.neurolive.neuro_live_backend.repository.BaseLineRepository;
import com.neurolive.neuro_live_backend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

@Service
@Transactional
// Calcula y consulta la linea base biometrica de cada paciente.
public class BaseLineService {

    private final BaseLineRepository baseLineRepository;
    private final UserRepository userRepository;

    public BaseLineService(BaseLineRepository baseLineRepository,
                           UserRepository userRepository) {
        this.baseLineRepository = baseLineRepository;
        this.userRepository = userRepository;
    }

    public BaseLine calculate(Long patientId, Collection<BiometricData> biometricSamples) {
        validatePatientReference(patientId);

        BaseLine baseLine = baseLineRepository.findByPatientId(patientId)
                .orElseGet(() -> new BaseLine(patientId));

        baseLine.calculate(biometricSamples);
        return baseLineRepository.save(baseLine);
    }

    public BaseLine updateFromTelemetry(Long patientId, Collection<BiometricData> biometricSamples) {
        return calculate(patientId, biometricSamples);
    }

    @Transactional(readOnly = true)
    public BaseLine findByPatientId(Long patientId) {
        validatePatientReference(patientId);

        return baseLineRepository.findByPatientId(patientId)
                .orElseThrow(() -> new EntityNotFoundException("Baseline not found for patient " + patientId));
    }

    private Patient validatePatientReference(Long patientId) {
        if (patientId == null || patientId <= 0) {
            throw new IllegalArgumentException("Patient reference must be a positive identifier");
        }

        User patient = userRepository.findById(patientId)
                .orElseThrow(() -> new EntityNotFoundException("Patient not found with id " + patientId));

        if (!(patient instanceof Patient typedPatient)) {
            throw new IllegalArgumentException("Referenced user is not a patient");
        }

        return typedPatient;
    }
}
