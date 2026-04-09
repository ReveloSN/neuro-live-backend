package com.neurolive.neuro_live_backend.domain.analysis;

import com.neurolive.neuro_live_backend.data.enums.StateEnum;
import com.neurolive.neuro_live_backend.domain.crisis.EmotionalState;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "keystroke_dynamics")
@Getter
@NoArgsConstructor
// Registra la dinamica basica del tecleo
public class KeystrokeDynamics {

    private static final float AT_RISK_DWELL_TIME_THRESHOLD = 180.0f;
    private static final float CRISIS_DWELL_TIME_THRESHOLD = 260.0f;
    private static final float AT_RISK_FLIGHT_TIME_THRESHOLD = 220.0f;
    private static final float CRISIS_FLIGHT_TIME_THRESHOLD = 320.0f;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(name = "dwell_time", nullable = false, updatable = false)
    private float dwellTime;

    @Column(name = "flight_time", nullable = false, updatable = false)
    private float flightTime;

    @Column(name = "session_id", length = 120, updatable = false)
    private String sessionId;

    @Column(name = "error_count", updatable = false)
    private Integer errorCount;

    @Column(name = "error_rate", updatable = false)
    private Float errorRate;

    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    private KeystrokeDynamics(Long userId,
                              String sessionId,
                              Float dwellTime,
                              Float flightTime,
                              Integer errorCount,
                              Float errorRate,
                              LocalDateTime timestamp) {
        this.userId = validateUserId(userId);
        this.sessionId = normalizeOptionalText(sessionId);
        this.dwellTime = validateTiming(dwellTime, "Dwell time");
        this.flightTime = validateTiming(flightTime, "Flight time");
        this.errorCount = validateErrorCount(errorCount);
        this.errorRate = validateErrorRate(errorRate);
        this.timestamp = resolveTimestamp(timestamp);
    }

    public static KeystrokeDynamics capture(Long userId,
                                            Float dwellTime,
                                            Float flightTime,
                                            LocalDateTime timestamp) {
        return new KeystrokeDynamics(userId, null, dwellTime, flightTime, null, null, timestamp);
    }

    public static KeystrokeDynamics capture(Long userId,
                                            String sessionId,
                                            Float dwellTime,
                                            Float flightTime,
                                            Integer errorCount,
                                            Float errorRate,
                                            LocalDateTime timestamp) {
        return new KeystrokeDynamics(userId, sessionId, dwellTime, flightTime, errorCount, errorRate, timestamp);
    }

    // Deja la señal lista para futura evaluacion de crisis
    public EmotionalState analyzePattern() {
        if (dwellTime >= CRISIS_DWELL_TIME_THRESHOLD
                || flightTime >= CRISIS_FLIGHT_TIME_THRESHOLD
                || (errorRate != null && errorRate >= 0.25f)) {
            return EmotionalState.from(StateEnum.ACTIVE_CRISIS);
        }
        if (dwellTime >= AT_RISK_DWELL_TIME_THRESHOLD
                || flightTime >= AT_RISK_FLIGHT_TIME_THRESHOLD
                || (errorRate != null && errorRate >= 0.15f)) {
            return EmotionalState.from(StateEnum.RISK_ELEVATED);
        }
        return EmotionalState.from(StateEnum.NORMAL);
    }

    @PrePersist
    @PreUpdate
    private void validateLifecycle() {
        validateUserId(userId);
        validateTiming(dwellTime, "Dwell time");
        validateTiming(flightTime, "Flight time");
        validateErrorCount(errorCount);
        validateErrorRate(errorRate);
        sessionId = normalizeOptionalText(sessionId);
        timestamp = resolveTimestamp(timestamp);
    }

    private Long validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("User reference must be a positive identifier");
        }
        return userId;
    }

    // Valida tiempos de escritura no negativos
    private float validateTiming(Float value, String fieldName) {
        if (value == null || !Float.isFinite(value) || value < 0.0f) {
            throw new IllegalArgumentException(fieldName + " must be a finite non-negative value");
        }
        return value;
    }

    private Integer validateErrorCount(Integer value) {
        if (value == null) {
            return null;
        }
        if (value < 0) {
            throw new IllegalArgumentException("Error count must be non-negative");
        }
        return value;
    }

    private Float validateErrorRate(Float value) {
        if (value == null) {
            return null;
        }
        if (!Float.isFinite(value) || value < 0.0f) {
            throw new IllegalArgumentException("Error rate must be a finite non-negative value");
        }
        return value;
    }

    private LocalDateTime resolveTimestamp(LocalDateTime timestamp) {
        return timestamp == null ? LocalDateTime.now() : timestamp;
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
