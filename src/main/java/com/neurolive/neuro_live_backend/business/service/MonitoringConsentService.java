package com.neurolive.neuro_live_backend.business.service;

import com.neurolive.neuro_live_backend.domain.user.Patient;
import com.neurolive.neuro_live_backend.domain.user.User;
import com.neurolive.neuro_live_backend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MonitoringConsentService {

    private final UserRepository userRepository;

    public MonitoringConsentService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public void assertMonitoringAllowed(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id " + userId));

        if (user instanceof Patient patient && !Boolean.TRUE.equals(patient.getConsentGiven())) {
            throw new IllegalStateException("Biometric consent is required before monitoring starts");
        }
    }

    public Patient registerPatientConsent(Long patientId) {
        User user = userRepository.findById(patientId)
                .orElseThrow(() -> new EntityNotFoundException("Patient not found with id " + patientId));

        if (!(user instanceof Patient patient)) {
            throw new IllegalArgumentException("Referenced user is not a patient");
        }

        patient.giveConsent();
        return (Patient) userRepository.save(patient);
    }
}
