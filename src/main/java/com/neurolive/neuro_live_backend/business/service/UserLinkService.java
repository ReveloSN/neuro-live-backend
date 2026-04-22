package com.neurolive.neuro_live_backend.business.service;

import com.neurolive.neuro_live_backend.data.enums.LinkTypeEnum;
import com.neurolive.neuro_live_backend.data.enums.RoleEnum;
import com.neurolive.neuro_live_backend.data.enums.StatusEnum;
import com.neurolive.neuro_live_backend.data.exception.UnauthorizedAccessException;
import com.neurolive.neuro_live_backend.domain.user.Patient;
import com.neurolive.neuro_live_backend.domain.user.User;
import com.neurolive.neuro_live_backend.domain.user.UserLink;
import com.neurolive.neuro_live_backend.repository.UserLinkRepository;
import com.neurolive.neuro_live_backend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@Transactional
// Administra los vinculos entre pacientes y usuarios relacionados.
public class UserLinkService {

    private final UserLinkRepository userLinkRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final Duration tokenLifetime;

    public UserLinkService(UserLinkRepository userLinkRepository,
                        UserRepository userRepository,
                        AuditLogService auditLogService,
                        @Value("${links.token-expiration-minutes:15}") long tokenExpirationMinutes) {
        this.userLinkRepository = userLinkRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
        this.tokenLifetime = validateTokenLifetime(tokenExpirationMinutes);
    }

    public UserLink createPendingLink(Long patientId, Long linkedUserId, LinkTypeEnum linkType) {
        Patient patient = getPatient(patientId);
        User linkedUser = getLinkedUser(linkedUserId, linkType);

        UserLink userLink = new UserLink(patient, linkedUser, linkType);
        userLink.generateToken(LocalDateTime.now().plus(tokenLifetime));
        return userLinkRepository.save(userLink);
    }

    public UserLink activate(String token) {
        UserLink userLink = userLinkRepository.findByToken(normalizeToken(token))
                .orElseThrow(() -> new EntityNotFoundException("Link token not found"));

        if (!userLink.validateToken(token, LocalDateTime.now())) {
            throw new IllegalArgumentException("Invalid link token");
        }
        if (userLink.getLinkedUser() == null) {
            throw new IllegalStateException("Link token must be redeemed by an authenticated caregiver or doctor");
        }

        userLink.activate();
        return userLinkRepository.save(userLink);
    }

    public UserLink issueToken(String requesterEmail, String ipOrigin) {
        User requester = resolveCurrentUser(requesterEmail);
        if (!(requester instanceof Patient patient)) {
            throw new UnauthorizedAccessException("Only patients can generate link tokens");
        }

        revokePendingTokens(patient.getId());

        UserLink userLink = new UserLink(patient);
        userLink.generateToken(LocalDateTime.now().plus(tokenLifetime));
        UserLink savedLink = userLinkRepository.save(userLink);
        auditLogService.record(requester.getId(), "GENERATE_LINK_TOKEN", patient.getId(), normalizeIp(ipOrigin));
        return savedLink;
    }

    public UserLink redeemToken(String requesterEmail, String token, String ipOrigin) {
        User requester = resolveCurrentUser(requesterEmail);
        if (requester.getRole() != RoleEnum.CAREGIVER && requester.getRole() != RoleEnum.DOCTOR) {
            throw new UnauthorizedAccessException("Only caregivers and doctors can redeem link tokens");
        }

        String normalizedToken = normalizeToken(token);
        UserLink userLink = userLinkRepository.findByToken(normalizedToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid link token"));

        if (userLink.getPatientId() != null && userLink.getPatientId().equals(requester.getId())) {
            throw new IllegalArgumentException("Patient cannot redeem their own link token");
        }
        if (userLink.getStatus() == StatusEnum.REVOKED) {
            throw new IllegalStateException("Link token is no longer active");
        }
        if (userLink.getConsumedAt() != null || userLink.getStatus() == StatusEnum.ACTIVE) {
            throw new IllegalStateException("Link token has already been used");
        }
        if (userLink.getExpiresAt() != null && !userLink.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new IllegalStateException("Link token has expired");
        }
        if (userLink.getLinkedUserId() != null && !userLink.getLinkedUserId().equals(requester.getId())) {
            throw new UnauthorizedAccessException("Link token belongs to a different user");
        }
        if (hasActiveLink(userLink.getPatientId(), requester.getId())) {
            throw new IllegalStateException("A link between the current user and patient already exists");
        }

        userLink.activate(requester, LocalDateTime.now());
        UserLink savedLink = userLinkRepository.save(userLink);
        auditLogService.record(requester.getId(), "REDEEM_LINK_TOKEN", userLink.getPatientId(), normalizeIp(ipOrigin));
        return savedLink;
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

    @Transactional(readOnly = true)
    public List<UserLink> getLinksForCurrentUser(String requesterEmail) {
        User requester = resolveCurrentUser(requesterEmail);

        return switch (requester.getRole()) {
            case PATIENT -> userLinkRepository.findAllByPatient_IdOrderByCreatedAtDesc(requester.getId());
            case CAREGIVER, DOCTOR -> userLinkRepository.findAllByLinkedUser_IdOrderByCreatedAtDesc(requester.getId());
            default -> throw new UnauthorizedAccessException("Current user does not support account links");
        };
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
        return token.trim().toUpperCase(Locale.ROOT);
    }

    private User resolveCurrentUser(String requesterEmail) {
        if (requesterEmail == null || requesterEmail.isBlank()) {
            throw new IllegalArgumentException("Authenticated email is required");
        }

        return userRepository.findByEmail(requesterEmail.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new EntityNotFoundException("Authenticated user not found"));
    }

    private void revokePendingTokens(Long patientId) {
        userLinkRepository.findAllByPatient_IdAndStatusOrderByCreatedAtDesc(patientId, StatusEnum.PENDING)
                .forEach(UserLink::revoke);
    }

    private Duration validateTokenLifetime(long tokenExpirationMinutes) {
        if (tokenExpirationMinutes <= 0) {
            throw new IllegalArgumentException("Link token expiration must be positive");
        }
        return Duration.ofMinutes(tokenExpirationMinutes);
    }

    private String normalizeIp(String ipOrigin) {
        return ipOrigin == null || ipOrigin.isBlank() ? "unknown" : ipOrigin.trim();
    }
}
