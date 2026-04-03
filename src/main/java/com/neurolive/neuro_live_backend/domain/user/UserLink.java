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

import java.time.LocalDateTime;

@Entity
@Table(name = "user_links")
@Getter
@NoArgsConstructor
// Modela la relacion entre un paciente y otro usuario vinculado.
public class UserLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "linked_user_id", nullable = false)
    private User linkedUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "link_type", nullable = false, length = 20)
    private LinkTypeEnum linkType;

    @Column(nullable = false, unique = true, length = 32)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusEnum status = StatusEnum.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

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
        validatePatient(patient);
        token = patient.generateLinkToken();
        status = StatusEnum.PENDING;
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        return token;
    }

    public boolean validateToken() {
        return token != null && !token.isBlank() && status == StatusEnum.PENDING;
    }

    public boolean validateToken(String providedToken) {
        if (providedToken == null || providedToken.isBlank()) {
            return false;
        }
        return validateToken() && token.equals(providedToken.trim());
    }

    public void activate() {
        if (!validateToken()) {
            throw new IllegalStateException("Link token is not valid for activation");
        }
        status = StatusEnum.ACTIVE;
    }

    public void revoke() {
        status = StatusEnum.REVOKED;
    }

    @PrePersist
    private void initialize() {
        validatePatient(patient);
        validateLinkedUser(linkedUser, linkType);
        validateLinkType(linkType);
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
        if (linkType == null) {
            return linkedUser;
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
}
