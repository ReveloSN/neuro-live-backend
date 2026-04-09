package com.neurolive.neuro_live_backend.domain.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Locale;

@Entity
@Table(name = "account_recovery_tokens")
@Getter
@NoArgsConstructor
public class AccountRecoveryToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(nullable = false, length = 150, updatable = false)
    private String email;

    @Column(name = "token_hash", nullable = false, length = 128, updatable = false)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private LocalDateTime expiresAt;

    @Column(name = "consumed_at")
    private LocalDateTime consumedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private AccountRecoveryToken(Long userId, String email, String tokenHash, LocalDateTime expiresAt) {
        this.userId = validateIdentifier(userId, "User");
        this.email = normalizeEmail(email);
        this.tokenHash = requireText(tokenHash, "Token hash");
        this.expiresAt = requireTimestamp(expiresAt, "Expiration time");
        this.createdAt = LocalDateTime.now();
    }

    public static AccountRecoveryToken issue(Long userId, String email, String tokenHash, LocalDateTime expiresAt) {
        return new AccountRecoveryToken(userId, email, tokenHash, expiresAt);
    }

    public boolean isActive(LocalDateTime referenceTime) {
        LocalDateTime effectiveReferenceTime = requireTimestamp(referenceTime, "Reference time");
        return consumedAt == null && expiresAt.isAfter(effectiveReferenceTime);
    }

    public boolean matches(String candidateHash) {
        return tokenHash != null && tokenHash.equals(requireText(candidateHash, "Candidate token hash"));
    }

    public void consume(LocalDateTime consumedAt) {
        if (!isActive(consumedAt)) {
            throw new IllegalStateException("Recovery token is already expired or consumed");
        }
        this.consumedAt = requireTimestamp(consumedAt, "Consumed time");
    }

    @PrePersist
    private void initialize() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    private Long validateIdentifier(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + " identifier must be positive");
        }
        return value;
    }

    private String normalizeEmail(String email) {
        String normalized = requireText(email, "Email").toLowerCase(Locale.ROOT);
        return normalized;
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private LocalDateTime requireTimestamp(LocalDateTime value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value;
    }
}
