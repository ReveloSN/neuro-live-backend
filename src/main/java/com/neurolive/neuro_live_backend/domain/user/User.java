package com.neurolive.neuro_live_backend.domain.user;

import com.neurolive.neuro_live_backend.data.enums.RoleEnum;
import com.neurolive.neuro_live_backend.data.exception.UnauthorizedAccessException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.function.Supplier;

@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@NoArgsConstructor
// Clase base que define el comportamiento comun de cualquier usuario del sistema.
public abstract class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RoleEnum role;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "photo_url")
    private String photoUrl;

    protected User(RoleEnum role) {
        this.role = role;
    }

    public static User createForRole(RoleEnum role) {
        if (role == null) {
            throw new IllegalArgumentException("Role is required");
        }

        return switch (role) {
            case USER_PERSONAL -> new PersonalUser();
            case PATIENT -> new Patient();
            case CAREGIVER -> new Caregiver();
            case DOCTOR -> new Doctor();
        };
    }

    public void register(String name, String email, String passwordHash) {
        register(name, email, passwordHash, null);
    }

    public void register(String name, String email, String passwordHash, String photoUrl) {
        this.name = validateName(name);
        this.email = normalizeEmail(email);
        this.passwordHash = validatePasswordHash(passwordHash);
        this.photoUrl = normalizeOptionalText(photoUrl);
        this.isActive = true;
        this.createdAt = LocalDateTime.now();
        validateRole();
    }

    public String authenticate(String rawPassword,
                               Predicate<String> passwordMatches,
                               Supplier<String> sessionTokenSupplier) {
        if (!Boolean.TRUE.equals(isActive)) {
            throw new IllegalStateException("User is inactive");
        }
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }
        if (passwordMatches == null) {
            throw new IllegalArgumentException("Password validation is required");
        }
        if (!passwordMatches.test(rawPassword)) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        if (sessionTokenSupplier == null) {
            throw new IllegalArgumentException("Session token generation is required");
        }

        return sessionTokenSupplier.get();
    }

    public void updateProfile(String name, String photoUrl) {
        this.name = validateName(name);
        this.photoUrl = normalizeOptionalText(photoUrl);
    }

    public void recoverAccount(String passwordHash) {
        this.passwordHash = validatePasswordHash(passwordHash);
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public Boolean getActive() {
        return isActive;
    }

    protected void validateLinkedPatientAccess(Long patientId, java.util.List<UserLink> links) {
        if (patientId == null || patientId <= 0) {
            throw new IllegalArgumentException("Patient reference must be a positive identifier");
        }
        if (links == null || links.stream().noneMatch(link ->
                link.getStatus() == com.neurolive.neuro_live_backend.data.enums.StatusEnum.ACTIVE
                        && link.getPatientId() != null
                        && link.getPatientId().equals(patientId))) {
            throw new UnauthorizedAccessException("User is not linked to the requested patient");
        }
    }

    protected abstract RoleEnum supportedRole();

    @PrePersist
    @PreUpdate
    private void validateRole() {
        if (role == null) {
            role = supportedRole();
        }
        if (role != supportedRole()) {
            throw new IllegalStateException("User subtype does not match the assigned role");
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    private String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }
        return name.trim();
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String validatePasswordHash(String passwordHash) {
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("Password hash is required");
        }
        return passwordHash.trim();
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
