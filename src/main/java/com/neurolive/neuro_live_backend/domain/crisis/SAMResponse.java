package com.neurolive.neuro_live_backend.domain.crisis;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "sam_responses")
@Getter
@NoArgsConstructor
public class SAMResponse {

    private static final int SAM_MIN_VALUE = 1;
    private static final int SAM_MAX_VALUE = 9;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_id", nullable = false, updatable = false)
    private Long patientId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "crisis_event_id", nullable = false, unique = true)
    private CrisisEvent crisisEvent;

    @Column(nullable = false, updatable = false)
    private Integer valence;

    @Column(nullable = false, updatable = false)
    private Integer arousal;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private LocalDateTime recordedAt;

    private SAMResponse(CrisisEvent crisisEvent, Integer valence, Integer arousal, LocalDateTime recordedAt) {
        this.crisisEvent = validateCrisisEvent(crisisEvent);
        this.patientId = crisisEvent.getPatientId();
        this.valence = validateScaleValue(valence, "SAM valence");
        this.arousal = validateScaleValue(arousal, "SAM arousal");
        this.recordedAt = recordedAt == null ? LocalDateTime.now() : recordedAt;
    }

    // Conserva la fecha real en que se registro la respuesta
    public static SAMResponse create(CrisisEvent crisisEvent,
                                     Integer valence,
                                     Integer arousal,
                                     LocalDateTime recordedAt) {
        return new SAMResponse(crisisEvent, valence, arousal, recordedAt);
    }

    @PrePersist
    @PreUpdate
    private void validateLifecycle() {
        validateCrisisEvent(crisisEvent);

        if (patientId == null || patientId <= 0) {
            throw new IllegalStateException("Patient reference must be a positive identifier");
        }
        if (!patientId.equals(crisisEvent.getPatientId())) {
            throw new IllegalStateException("SAM response patient must match the crisis event patient");
        }

        validateScaleValue(valence, "SAM valence");
        validateScaleValue(arousal, "SAM arousal");

        if (recordedAt == null) {
            recordedAt = LocalDateTime.now();
        }
    }

    boolean belongsTo(CrisisEvent crisisEvent) {
        if (crisisEvent == null || this.crisisEvent == null) {
            return false;
        }
        if (this.crisisEvent == crisisEvent) {
            return true;
        }
        return this.crisisEvent.getId() != null
                && crisisEvent.getId() != null
                && this.crisisEvent.getId().equals(crisisEvent.getId());
    }

    private CrisisEvent validateCrisisEvent(CrisisEvent crisisEvent) {
        if (crisisEvent == null) {
            throw new IllegalArgumentException("Crisis event is required");
        }
        if (crisisEvent.isActive()) {
            throw new IllegalStateException("SAM response can only be created for a closed crisis event");
        }
        if (crisisEvent.getInterventionType() == null) {
            throw new IllegalStateException("SAM response requires a completed intervention");
        }
        return crisisEvent;
    }

    // Valida que la escala SAM este dentro del rango permitido
    private Integer validateScaleValue(Integer value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        if (value < SAM_MIN_VALUE || value > SAM_MAX_VALUE) {
            throw new IllegalArgumentException(fieldName + " must be between 1 and 9");
        }
        return value;
    }
}
