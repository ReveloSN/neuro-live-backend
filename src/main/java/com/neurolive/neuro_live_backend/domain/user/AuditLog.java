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

@Entity
@Table(name = "audit_logs")
@Getter
@NoArgsConstructor
// Guarda un historial de acciones relevantes para auditoria y seguimiento.
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 150)
    private String action;

    @Column(name = "target_patient_id", nullable = false)
    private Long targetPatientId;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "ip_origin", nullable = false, length = 60)
    private String ipOrigin;

    public void record(Long userId, String action, Long targetPatientId, String ipOrigin) {
        record(userId, action, targetPatientId, ipOrigin, LocalDateTime.now());
    }

    public void record(Long userId, String action, Long targetPatientId, String ipOrigin, LocalDateTime timestamp) {
        this.userId = validateIdentifier(userId, "User");
        this.action = validateAction(action);
        this.targetPatientId = validateIdentifier(targetPatientId, "Target patient");
        this.ipOrigin = validateIpOrigin(ipOrigin);
        this.timestamp = timestamp == null ? LocalDateTime.now() : timestamp;
    }

    @PrePersist
    private void initializeTimestamp() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }

    private Long validateIdentifier(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + " identifier must be positive");
        }
        return value;
    }

    private String validateAction(String action) {
        if (action == null || action.isBlank()) {
            throw new IllegalArgumentException("Action is required");
        }
        return action.trim();
    }

    private String validateIpOrigin(String ipOrigin) {
        if (ipOrigin == null || ipOrigin.isBlank()) {
            throw new IllegalArgumentException("IP origin is required");
        }
        return ipOrigin.trim();
    }
}
