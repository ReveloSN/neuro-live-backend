package com.neurolive.neuro_live_backend.domain.crisis;

import com.neurolive.neuro_live_backend.data.enums.StateEnum;
import com.neurolive.neuro_live_backend.data.enums.TypeEnum;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;

@Entity
@Table(name = "crisis_events")
@Getter
@NoArgsConstructor
// Registra el ciclo de vida de un episodio de crisis detectado.
public class CrisisEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "started_at", nullable = false, updatable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StateEnum state;

    @Enumerated(EnumType.STRING)
    @Column(name = "intervention_type", length = 40)
    private TypeEnum interventionType;

    @Column(name = "trigger_bpm")
    private Float triggerBpm;

    @Column(name = "trigger_spo2")
    private Float triggerSpo2;

    @Column(name = "typing_error_rate")
    private Float typingErrorRate;

    @Column(name = "typing_dwell_time")
    private Float typingDwellTime;

    @Column(name = "typing_flight_time")
    private Float typingFlightTime;

    @Column(name = "typing_error_count")
    private Integer typingErrorCount;

    @OneToOne(mappedBy = "crisisEvent", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private InterventionProtocol interventionProtocol;

    @OneToOne(mappedBy = "crisisEvent", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private SAMResponse samResponse;

    private CrisisEvent(Long patientId, StateEnum state, LocalDateTime startedAt) {
        this.patientId = validatePatientId(patientId);
        this.state = validateOpenState(state);
        this.startedAt = requireStartedAt(startedAt);
    }

    // Crea el evento cuando la crisis ya fue detectada
    public static CrisisEvent open(Long patientId, StateEnum state, LocalDateTime startedAt) {
        return new CrisisEvent(patientId, state, startedAt);
    }

    // Valida que la crisis no termine antes de empezar
    public void close(LocalDateTime endedAt, StateEnum finalState, TypeEnum interventionType) {
        if (!isActive()) {
            throw new IllegalStateException("Crisis event is already closed");
        }

        this.endedAt = validateEndedAt(endedAt);
        this.state = validateClosedState(finalState);
        this.interventionType = resolveInterventionType(interventionType);
        validateLifecycle();
    }

    // Mantiene compatibilidad con el registro directo de SAM
    public void attachSamData(Integer samValence, Integer samArousal) {
        attachSamResponse(SAMResponse.create(this, samValence, samArousal, null));
    }

    // Mantiene el protocolo asociado al evento
    public void attachInterventionProtocol(InterventionProtocol interventionProtocol) {
        if (interventionProtocol == null) {
            throw new IllegalArgumentException("Intervention protocol is required");
        }
        if (this.interventionProtocol != null) {
            throw new IllegalStateException("Intervention protocol is already attached to this crisis event");
        }
        TypeEnum protocolType = interventionProtocol.getType().canonical();
        if (interventionType != null && !interventionType.sameFamily(protocolType)) {
            throw new IllegalArgumentException("Intervention protocol type must match the crisis event type");
        }

        interventionProtocol.attachTo(this);
        this.interventionType = protocolType;
        this.interventionProtocol = interventionProtocol;
        validateLifecycle();
    }

    // Relaciona la respuesta emocional con el evento de crisis
    public void attachSamResponse(SAMResponse samResponse) {
        if (samResponse == null) {
            throw new IllegalArgumentException("SAM response is required");
        }
        if (isActive()) {
            throw new IllegalStateException("SAM response can only be attached to a closed crisis event");
        }
        if (interventionType == null) {
            throw new IllegalStateException("SAM data can only be attached after assigning an intervention type");
        }
        if (this.samResponse != null) {
            throw new IllegalStateException("SAM response is already attached to this crisis event");
        }
        if (!samResponse.belongsTo(this)) {
            throw new IllegalArgumentException("SAM response does not belong to this crisis event");
        }

        this.samResponse = samResponse;
        validateLifecycle();
    }

    public void recordTriggerMetrics(Float triggerBpm,
                                    Float triggerSpo2,
                                    Float typingErrorRate,
                                    Float typingDwellTime,
                                    Float typingFlightTime,
                                    Integer typingErrorCount) {
        this.triggerBpm = validateOptionalMetric(triggerBpm, "Trigger BPM");
        this.triggerSpo2 = validateOptionalMetric(triggerSpo2, "Trigger SpO2");
        this.typingErrorRate = validateOptionalMetric(typingErrorRate, "Typing error rate");
        this.typingDwellTime = validateOptionalMetric(typingDwellTime, "Typing dwell time");
        this.typingFlightTime = validateOptionalMetric(typingFlightTime, "Typing flight time");
        this.typingErrorCount = validateOptionalCount(typingErrorCount, "Typing error count");
    }

    public boolean isActive() {
        return endedAt == null;
    }

    // Evita duplicar logica de crisis en varias clases
    public EmotionalState getEmotionalState() {
        return EmotionalState.from(state);
    }

    public Integer getSamValence() {
        return samResponse == null ? null : samResponse.getValence();
    }

    public Integer getSamArousal() {
        return samResponse == null ? null : samResponse.getArousal();
    }

    public Duration calculateDuration() {
        LocalDateTime referenceTime = endedAt == null ? LocalDateTime.now() : endedAt;
        return calculateDuration(referenceTime);
    }

    // Usa un punto de referencia seguro para medir la duracion
    public Duration calculateDuration(LocalDateTime referenceTime) {
        LocalDateTime effectiveEnd = endedAt == null ? validateReferenceTime(referenceTime) : endedAt;
        return Duration.between(startedAt, effectiveEnd);
    }

    @PrePersist
    @PreUpdate
    private void validateLifecycle() {
        validatePatientId(patientId);
        requireStartedAt(startedAt);

        if (endedAt != null && endedAt.isBefore(startedAt)) {
            throw new IllegalStateException("Crisis event cannot end before it starts");
        }
        if (endedAt == null && state == StateEnum.NORMAL) {
            throw new IllegalStateException("Active crisis event cannot be in a normal state");
        }
        if (endedAt != null && state == StateEnum.ACTIVE_CRISIS) {
            throw new IllegalStateException("Closed crisis event cannot remain in active crisis state");
        }
        if (interventionProtocol != null && interventionType == null) {
            throw new IllegalStateException("Intervention protocol requires an intervention type");
        }
        if (interventionProtocol != null && !interventionProtocol.belongsTo(this)) {
            throw new IllegalStateException("Intervention protocol must belong to the same crisis event");
        }
        if (interventionProtocol != null && !interventionType.sameFamily(interventionProtocol.getType())) {
            throw new IllegalStateException("Intervention protocol type must match the crisis event type");
        }
        if (samResponse != null && interventionType == null) {
            throw new IllegalStateException("SAM data requires an intervention type");
        }
        if (samResponse != null && isActive()) {
            throw new IllegalStateException("Active crisis event cannot have SAM data");
        }
        if (samResponse != null && !samResponse.belongsTo(this)) {
            throw new IllegalStateException("SAM response must belong to the same crisis event");
        }
    }

    private Long validatePatientId(Long patientId) {
        if (patientId == null || patientId <= 0) {
            throw new IllegalArgumentException("Patient reference must be a positive identifier");
        }
        return patientId;
    }

    private LocalDateTime requireStartedAt(LocalDateTime startedAt) {
        if (startedAt == null) {
            throw new IllegalArgumentException("Crisis start time is required");
        }
        return startedAt;
    }

    private LocalDateTime validateEndedAt(LocalDateTime endedAt) {
        if (endedAt == null) {
            throw new IllegalArgumentException("Crisis end time is required");
        }
        if (endedAt.isBefore(startedAt)) {
            throw new IllegalArgumentException("Crisis event cannot end before it starts");
        }
        return endedAt;
    }

    private LocalDateTime validateReferenceTime(LocalDateTime referenceTime) {
        if (referenceTime == null) {
            throw new IllegalArgumentException("Reference time is required");
        }
        if (referenceTime.isBefore(startedAt)) {
            throw new IllegalArgumentException("Reference time cannot be before the crisis start time");
        }
        return referenceTime;
    }

    private StateEnum validateOpenState(StateEnum state) {
        if (state == null) {
            throw new IllegalArgumentException("Crisis state is required");
        }
        if (state == StateEnum.NORMAL) {
            throw new IllegalArgumentException("A crisis event cannot start in a normal state");
        }
        return state;
    }

    private StateEnum validateClosedState(StateEnum state) {
        if (state == null) {
            throw new IllegalArgumentException("Final crisis state is required");
        }
        if (state == StateEnum.ACTIVE_CRISIS) {
            throw new IllegalArgumentException("A closed crisis event cannot remain in active crisis state");
        }
        return state;
    }

    private TypeEnum validateInterventionType(TypeEnum interventionType) {
        if (interventionType == null) {
            throw new IllegalArgumentException("Intervention type is required");
        }
        return interventionType.canonical();
    }

    private TypeEnum resolveInterventionType(TypeEnum interventionType) {
        TypeEnum validatedInterventionType = validateInterventionType(interventionType);

        if (this.interventionType != null && !this.interventionType.sameFamily(validatedInterventionType)) {
            throw new IllegalArgumentException("Final intervention type must match the active crisis intervention");
        }

        return validatedInterventionType;
    }

    private Float validateOptionalMetric(Float value, String fieldName) {
        if (value == null) {
            return null;
        }
        if (!Float.isFinite(value) || value < 0.0f) {
            throw new IllegalArgumentException(fieldName + " must be a finite non-negative value");
        }
        return value;
    }

    private Integer validateOptionalCount(Integer value, String fieldName) {
        if (value == null) {
            return null;
        }
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + " must be non-negative");
        }
        return value;
    }
}
