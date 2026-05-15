package com.neurolive.neuro_live_backend.business.service;

import com.neurolive.neuro_live_backend.domain.user.Doctor;
import com.neurolive.neuro_live_backend.domain.user.Patient;
import com.neurolive.neuro_live_backend.domain.user.User;
import com.neurolive.neuro_live_backend.presentation.dto.UpdateUserProfileRequest;
import com.neurolive.neuro_live_backend.presentation.dto.UserProfileResponse;
import com.neurolive.neuro_live_backend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
// Gestiona la consulta, actualizacion y recuperacion de cuentas de usuario.
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUserProfile(String email) {
        User user = findByEmailOrThrow(email);
        return toProfileResponse(user);
    }

    public UserProfileResponse updateCurrentUserProfile(String email, UpdateUserProfileRequest request) {
        User user = findByEmailOrThrow(email);
        user.updateProfile(request.getName(), request.getPhotoUrl());
        return toProfileResponse(userRepository.save(user));
    }

    public void recoverAccount(String email, String newPassword) {
        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("New password is required");
        }

        User user = findByEmailOrThrow(email);
        user.recoverAccount(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public String getDoctorSpecialty(String email) {
        User user = findByEmailOrThrow(email);
        if (!(user instanceof Doctor doctor)) {
            throw new IllegalArgumentException("User is not a doctor");
        }
        return doctor.getSpecialty();
    }

    public String updateDoctorSpecialty(String email, String specialty) {
        User user = findByEmailOrThrow(email);
        if (!(user instanceof Doctor doctor)) {
            throw new IllegalArgumentException("User is not a doctor");
        }
        doctor.updateSpecialty(specialty);
        userRepository.save(doctor);
        return doctor.getSpecialty();
    }

    @Transactional(readOnly = true)
    public PatientConsentResponse getPatientConsent(String requesterEmail, Long patientId) {
        User patient = userRepository.findById(patientId)
                .orElseThrow(() -> new EntityNotFoundException("Patient not found with id " + patientId));
        if (!(patient instanceof Patient typedPatient)) {
            throw new IllegalArgumentException("Referenced user is not a patient");
        }
        return new PatientConsentResponse(typedPatient.getId(), typedPatient.getConsentGiven(), typedPatient.getConsentDate());
    }

    public PatientConsentResponse grantPatientConsent(String requesterEmail, Long patientId) {
        User patient = userRepository.findById(patientId)
                .orElseThrow(() -> new EntityNotFoundException("Patient not found with id " + patientId));
        if (!(patient instanceof Patient typedPatient)) {
            throw new IllegalArgumentException("Referenced user is not a patient");
        }
        typedPatient.giveConsent();
        userRepository.save(typedPatient);
        return new PatientConsentResponse(typedPatient.getId(), typedPatient.getConsentGiven(), typedPatient.getConsentDate());
    }

    public User findByEmailOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    private UserProfileResponse toProfileResponse(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                user.getActive()
        );
    }

    public record PatientConsentResponse(Long patientId, Boolean consentGiven, java.time.LocalDateTime consentDate) {}
}
