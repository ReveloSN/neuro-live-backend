package com.neurolive.neuro_live_backend.domain.crisis;

import com.neurolive.neuro_live_backend.data.enums.TypeEnum;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "intervention_protocols")
@Getter
@NoArgsConstructor
public class InterventionProtocol {

    private static final int MIN_LIGHT_INTENSITY = 1;
    private static final int MAX_LIGHT_INTENSITY = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TypeEnum type;

    @Column(nullable = false)
    private Boolean active = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "light_color", length = 40)
    private String lightColor;

    @Column(name = "light_intensity")
    private Integer lightIntensity;

    @Column(name = "audio_track", length = 255)
    private String audioTrack;

    @Column(name = "ui_reduction_enabled")
    private Boolean uiReductionEnabled;

    @Column(name = "breathing_enabled")
    private Boolean breathingEnabled;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "crisis_event_id", nullable = false, unique = true)
    private CrisisEvent crisisEvent;

    private InterventionProtocol(Builder builder) {
        this.type = validateType(builder.type);
        this.active = builder.active;
        this.createdAt = builder.createdAt == null ? LocalDateTime.now() : builder.createdAt;
        this.lightColor = normalizeOptionalText(builder.lightColor);
        this.lightIntensity = builder.lightIntensity;
        this.audioTrack = normalizeOptionalText(builder.audioTrack);
        this.uiReductionEnabled = builder.uiReductionEnabled;
        this.breathingEnabled = builder.breathingEnabled;
        validateConfiguration();
    }

    // Construye el protocolo de forma segura
    public static Builder builder(TypeEnum type) {
        return new Builder(type);
    }

    // Activa la intervencion seleccionada
    public void activate() {
        active = true;
    }

    public void deactivate() {
        active = false;
    }

    void attachTo(CrisisEvent crisisEvent) {
        if (crisisEvent == null) {
            throw new IllegalArgumentException("Crisis event is required");
        }
        if (this.crisisEvent != null && !belongsTo(crisisEvent)) {
            throw new IllegalStateException("Intervention protocol is already attached to another crisis event");
        }

        this.crisisEvent = crisisEvent;
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

    @PrePersist
    @PreUpdate
    private void validateLifecycle() {
        validateType(type);

        if (active == null) {
            active = false;
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }

        validateConfiguration();

        if (crisisEvent == null) {
            throw new IllegalStateException("Intervention protocol must be attached to a crisis event");
        }
        if (crisisEvent.getInterventionType() != type) {
            throw new IllegalStateException("Intervention protocol type must match the crisis event type");
        }
    }

    // Valida la configuracion opcional segun el tipo
    private void validateConfiguration() {
        validateLightConfiguration();
        validateAudioConfiguration();
        validateUiConfiguration();
        validateBreathingConfiguration();
    }

    private void validateLightConfiguration() {
        boolean hasLightConfig = lightColor != null || lightIntensity != null;

        if (type == TypeEnum.LIGHTING_CONTROL) {
            if (lightColor == null || lightIntensity == null) {
                throw new IllegalStateException("Lighting control requires light color and intensity");
            }
            validateLightIntensity(lightIntensity);
            return;
        }

        if (hasLightConfig) {
            throw new IllegalStateException("Light configuration is only allowed for lighting control protocols");
        }
    }

    private void validateAudioConfiguration() {
        if (type == TypeEnum.AUDITORY_REGULATION) {
            if (audioTrack == null) {
                throw new IllegalStateException("Auditory regulation requires an audio track");
            }
            return;
        }

        if (audioTrack != null) {
            throw new IllegalStateException("Audio configuration is only allowed for auditory regulation protocols");
        }
    }

    private void validateUiConfiguration() {
        if (type == TypeEnum.UI_REDUCTION) {
            if (!Boolean.TRUE.equals(uiReductionEnabled)) {
                throw new IllegalStateException("UI reduction requires the UI flag to be enabled");
            }
            return;
        }

        if (uiReductionEnabled != null) {
            throw new IllegalStateException("UI configuration is only allowed for UI reduction protocols");
        }
    }

    private void validateBreathingConfiguration() {
        if (type == TypeEnum.GUIDED_BREATHING) {
            if (!Boolean.TRUE.equals(breathingEnabled)) {
                throw new IllegalStateException("Guided breathing requires the breathing flag to be enabled");
            }
            return;
        }

        if (breathingEnabled != null) {
            throw new IllegalStateException("Breathing configuration is only allowed for guided breathing protocols");
        }
    }

    private TypeEnum validateType(TypeEnum type) {
        if (type == null) {
            throw new IllegalArgumentException("Intervention type is required");
        }
        return type;
    }

    private int validateLightIntensity(Integer lightIntensity) {
        if (lightIntensity == null
                || lightIntensity < MIN_LIGHT_INTENSITY
                || lightIntensity > MAX_LIGHT_INTENSITY) {
            throw new IllegalArgumentException("Light intensity must be between 1 and 100");
        }
        return lightIntensity;
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    public static final class Builder {

        private final TypeEnum type;
        private Boolean active = false;
        private LocalDateTime createdAt;
        private String lightColor;
        private Integer lightIntensity;
        private String audioTrack;
        private Boolean uiReductionEnabled;
        private Boolean breathingEnabled;

        private Builder(TypeEnum type) {
            this.type = type;
        }

        public Builder active() {
            this.active = true;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder light(String lightColor, Integer lightIntensity) {
            this.lightColor = lightColor;
            this.lightIntensity = lightIntensity;
            return this;
        }

        public Builder audioTrack(String audioTrack) {
            this.audioTrack = audioTrack;
            return this;
        }

        public Builder uiReductionEnabled() {
            this.uiReductionEnabled = true;
            return this;
        }

        public Builder breathingEnabled() {
            this.breathingEnabled = true;
            return this;
        }

        public InterventionProtocol build() {
            return new InterventionProtocol(this);
        }
    }
}
