package com.neurolive.neuro_live_backend.business.service;

import com.neurolive.neuro_live_backend.data.enums.RoleEnum;
import com.neurolive.neuro_live_backend.data.enums.StatusEnum;
import com.neurolive.neuro_live_backend.data.exception.UnauthorizedAccessException;
import com.neurolive.neuro_live_backend.domain.user.Patient;
import com.neurolive.neuro_live_backend.domain.user.User;
import com.neurolive.neuro_live_backend.repository.UserLinkRepository;
import com.neurolive.neuro_live_backend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ClinicalAccessService {

    private final UserRepository userRepository;
    private final UserLinkRepository userLinkRepository;

    public ClinicalAccessService(UserRepository userRepository, UserLinkRepository userLinkRepository) {
        this.userRepository = userRepository;
        this.userLinkRepository = userLinkRepository;
    }

    public User resolveCurrentUser(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Authenticated email is required");
        }

        return userRepository.findByEmail(email.trim().toLowerCase(java.util.Locale.ROOT))
                .orElseThrow(() -> new EntityNotFoundException("Authenticated user not found"));
    }

    public User requirePatientAccess(String requesterEmail, Long patientId) {
        User requester = resolveCurrentUser(requesterEmail);
        validatePatientId(patientId);

        return switch (requester.getRole()) {
            case PATIENT, USER_PERSONAL -> requireSelfAccess(requester, patientId);
            case CAREGIVER, DOCTOR -> requireLinkedClinicalAccess(requester, patientId);
        };
    }

    public User requireThresholdManagement(String requesterEmail, Long targetUserId) {
        User requester = resolveCurrentUser(requesterEmail);

        if (requester.getRole() == RoleEnum.USER_PERSONAL && requester.getId().equals(targetUserId)) {
            return requester;
        }
        if (requester.getRole() == RoleEnum.DOCTOR
                && userLinkRepository.existsByPatient_IdAndLinkedUser_IdAndStatus(
                targetUserId,
                requester.getId(),
                StatusEnum.ACTIVE
        )) {
            return requester;
        }

        throw new UnauthorizedAccessException("User is not allowed to manage thresholds for the requested subject");
    }

    public Patient requirePatient(Long patientId) {
        User user = userRepository.findById(patientId)
                .orElseThrow(() -> new EntityNotFoundException("Patient not found with id " + patientId));

        if (!(user instanceof Patient patient)) {
            throw new IllegalArgumentException("Referenced user is not a patient");
        }

        return patient;
    }

    private User requireSelfAccess(User requester, Long patientId) {
        if (!requester.getId().equals(patientId)) {
            throw new UnauthorizedAccessException("User can only access their own clinical data");
        }
        return requester;
    }

    private User requireLinkedClinicalAccess(User requester, Long patientId) {
        boolean hasAccess = userLinkRepository.existsByPatient_IdAndLinkedUser_IdAndStatus(
                patientId,
                requester.getId(),
                StatusEnum.ACTIVE
        );

        if (!hasAccess) {
            throw new UnauthorizedAccessException("User is not linked to the requested patient");
        }

        return requester;
    }

    private void validatePatientId(Long patientId) {
        if (patientId == null || patientId <= 0) {
            throw new IllegalArgumentException("Patient reference must be a positive identifier");
        }
    }
}
