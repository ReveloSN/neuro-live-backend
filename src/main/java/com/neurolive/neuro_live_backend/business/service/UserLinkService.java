package com.neurolive.neuro_live_backend.business.service;

import com.neurolive.neuro_live_backend.data.enums.LinkTypeEnum;
import com.neurolive.neuro_live_backend.data.enums.RoleEnum;
import com.neurolive.neuro_live_backend.data.enums.StatusEnum;
import com.neurolive.neuro_live_backend.domain.user.Patient;
import com.neurolive.neuro_live_backend.domain.user.User;
import com.neurolive.neuro_live_backend.domain.user.UserLink;
import com.neurolive.neuro_live_backend.repository.UserLinkRepository;
import com.neurolive.neuro_live_backend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
// Administra los vinculos entre pacientes y usuarios relacionados.
public class UserLinkService {

    private final UserLinkRepository userLinkRepository;
    private final UserRepository userRepository;

    public UserLinkService(UserLinkRepository userLinkRepository,
                           UserRepository userRepository) {
        this.userLinkRepository = userLinkRepository;
        this.userRepository = userRepository;
    }

    public UserLink createPendingLink(Long patientId, Long linkedUserId, LinkTypeEnum linkType) {
        Patient patient = getPatient(patientId);
        User linkedUser = getLinkedUser(linkedUserId, linkType);

        UserLink userLink = new UserLink(patient, linkedUser, linkType);
        userLink.generateToken();
        return userLinkRepository.save(userLink);
    }

    public UserLink activate(String token) {
        UserLink userLink = userLinkRepository.findByToken(normalizeToken(token))
                .orElseThrow(() -> new EntityNotFoundException("Link token not found"));

        if (!userLink.validateToken(token)) {
            throw new IllegalArgumentException("Invalid link token");
        }

        userLink.activate();
        return userLinkRepository.save(userLink);
    }

    public UserLink revoke(Long linkId) {
        UserLink userLink = userLinkRepository.findById(linkId)
                .orElseThrow(() -> new EntityNotFoundException("User link not found"));

        userLink.revoke();
        return userLinkRepository.save(userLink);
    }

    @Transactional(readOnly = true)
    public boolean hasActiveLink(Long patientId, Long linkedUserId) {
        return userLinkRepository.existsByPatient_IdAndLinkedUser_IdAndStatus(patientId, linkedUserId, StatusEnum.ACTIVE);
    }

    @Transactional(readOnly = true)
    public List<UserLink> getActiveLinksForPatient(Long patientId) {
        return userLinkRepository.findAllByPatient_IdAndStatus(patientId, StatusEnum.ACTIVE);
    }

    private Patient getPatient(Long patientId) {
        User patient = userRepository.findByIdAndRole(patientId, RoleEnum.PATIENT)
                .orElseThrow(() -> new EntityNotFoundException("Patient not found with id " + patientId));

        return (Patient) patient;
    }

    private User getLinkedUser(Long linkedUserId, LinkTypeEnum linkType) {
        RoleEnum role = switch (linkType) {
            case CAREGIVER -> RoleEnum.CAREGIVER;
            case DOCTOR -> RoleEnum.DOCTOR;
        };

        return userRepository.findByIdAndRole(linkedUserId, role)
                .orElseThrow(() -> new EntityNotFoundException("Linked user not found with id " + linkedUserId));
    }

    private String normalizeToken(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Link token is required");
        }
        return token.trim().toUpperCase();
    }
}
