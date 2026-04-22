package com.neurolive.neuro_live_backend.domain.user;

import com.neurolive.neuro_live_backend.data.enums.LinkTypeEnum;
import com.neurolive.neuro_live_backend.data.enums.RoleEnum;
import com.neurolive.neuro_live_backend.data.enums.StatusEnum;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_links")
@Getter
@NoArgsConstructor
// Modela la relacion entre un paciente y otro usuario vinculado.
public class UserLink {

    private static final Duration DEFAULT_TOKEN_TTL = Duration.ofMinutes(15);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_user_id")
    private User linkedUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "link_type", length = 20)
    private LinkTypeEnum linkType;

    @Column(nullable = false, unique = true, length = 32)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusEnum status = StatusEnum.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "consumed_at")
    private LocalDateTime consumedAt;

    public UserLink(Patient patient) {
        this.patient = validatePatient(patient);
        this.status = StatusEnum.PENDING;
    }

    public UserLink(Patient patient, User linkedUser, LinkTypeEnum linkType) {
        this.patient = validatePatient(patient);
        this.linkedUser = validateLinkedUser(linkedUser, linkType);
        this.linkType = validateLinkType(linkType);
        this.status = StatusEnum.PENDING;
    }

    public Long getPatientId() {
        return patient == null ? null : patient.getId();
    }

    public Long getLinkedUserId() {
        return linkedUser == null ? null : linkedUser.getId();
    }

    public String generateToken() {
        return generateToken(LocalDateTime.now().plus(DEFAULT_TOKEN_TTL));
    }

    public String generateToken(LocalDateTime expiresAt) {
        validatePatient(patient);
        this.expiresAt = normalizeExpiration(expiresAt);
        this.consumedAt = null;
        token = patient.generateLinkToken();
        status = StatusEnum.PENDING;
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        return token;
    }

    public boolean validateToken() {
        return validateToken(LocalDateTime.now());
    }

    public boolean validateToken(LocalDateTime referenceTime) {
        return token != null
                && !token.isBlank()
                && status == StatusEnum.PENDING
                && consumedAt == null
                && !isExpired(referenceTime);
    }

    public boolean validateToken(String providedToken) {
        return validateToken(providedToken, LocalDateTime.now());
    }

    public boolean validateToken(String providedToken, LocalDateTime referenceTime) {
        if (providedToken == null || providedToken.isBlank()) {
            return false;
        }
        return validateToken(referenceTime) && token.equals(providedToken.trim().toUpperCase());
    }

    public void activate() {
        if (linkedUser == null) {
            throw new IllegalStateException("Linked user is required for activation");
        }
        activate(linkedUser, LocalDateTime.now());
    }

    public void activate(User linkedUser, LocalDateTime consumedAt) {
        if (!validateToken(consumedAt == null ? LocalDateTime.now() : consumedAt)) {
            throw new IllegalStateException("Link token is not valid for activation");
        }
        User validatedUser = validateLinkedUser(linkedUser, resolveLinkType(linkedUser));
        if (patient.getId() != null
                && validatedUser.getId() != null
                && patient.getId().equals(validatedUser.getId())) {
            throw new IllegalArgumentException("Patient cannot link to the same account");
        }
        this.linkedUser = validatedUser;
        this.linkType = resolveLinkType(validatedUser);
        status = StatusEnum.ACTIVE;
        this.consumedAt = consumedAt == null ? LocalDateTime.now() : consumedAt;
    }

    public void revoke() {
        status = StatusEnum.REVOKED;
    }

    @PrePersist
    private void initialize() {
        validatePatient(patient);
        validateLinkAssignmentConsistency();
        if (linkedUser != null) {
            this.linkedUser = validateLinkedUser(linkedUser, linkType);
            this.linkType = validateLinkType(linkType);
        }
        if (token == null) {
            generateToken();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    private Patient validatePatient(Patient patient) {
        if (patient == null) {
            throw new IllegalArgumentException("Patient is required");
        }
        return patient;
    }

    private User validateLinkedUser(User linkedUser, LinkTypeEnum linkType) {
        if (linkedUser == null) {
            throw new IllegalArgumentException("Linked user is required");
        }
        RoleEnum expectedRole = switch (linkType) {
            case CAREGIVER -> RoleEnum.CAREGIVER;
            case DOCTOR -> RoleEnum.DOCTOR;
        };

        if (linkedUser.getRole() != expectedRole) {
            throw new IllegalArgumentException("Linked user role does not match the link type");
        }

        return linkedUser;
    }

    private LinkTypeEnum validateLinkType(LinkTypeEnum linkType) {
        if (linkType == null) {
            throw new IllegalArgumentException("Link type is required");
        }
        return linkType;
    }

    private void validateLinkAssignmentConsistency() {
        if ((linkedUser == null && linkType != null) || (linkedUser != null && linkType == null)) {
            throw new IllegalStateException("Linked user and link type must be defined together");
        }
    }

    private LinkTypeEnum resolveLinkType(User linkedUser) {
        if (linkedUser == null) {
            throw new IllegalArgumentException("Linked user is required");
        }

        return switch (linkedUser.getRole()) {
            case CAREGIVER -> LinkTypeEnum.CAREGIVER;
            case DOCTOR -> LinkTypeEnum.DOCTOR;
            default -> throw new IllegalArgumentException("Only caregivers and doctors can link to a patient");
        };
    }

    private LocalDateTime normalizeExpiration(LocalDateTime expiresAt) {
        if (expiresAt == null) {
            throw new IllegalArgumentException("Token expiration is required");
        }
        if (!expiresAt.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Token expiration must be in the future");
        }
        return expiresAt;
    }

    private boolean isExpired(LocalDateTime referenceTime) {
        if (expiresAt == null) {
            return false;
        }
        LocalDateTime effectiveReference = referenceTime == null ? LocalDateTime.now() : referenceTime;
        return !expiresAt.isAfter(effectiveReference);
    }
}
