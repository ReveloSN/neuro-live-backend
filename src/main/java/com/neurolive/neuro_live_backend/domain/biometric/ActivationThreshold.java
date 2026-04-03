package com.neurolive.neuro_live_backend.domain.biometric;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "activation_thresholds")
@Getter
@NoArgsConstructor
// Define los umbrales biometricos que disparan alertas o activaciones.
public class ActivationThreshold {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bpm_min")
    private Float bpmMin;

    @Column(name = "bpm_max")
    private Float bpmMax;

    @Column(name = "spo2_min")
    private Float spo2Min;

    @Column(name = "error_rate_max")
    private Float errorRateMax;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public ActivationThreshold(Float bpmMin, Float bpmMax, Float spo2Min, Float errorRateMax) {
        this.bpmMin = validateMetric(bpmMin, "Minimum BPM");
        this.bpmMax = validateMetric(bpmMax, "Maximum BPM");
        this.spo2Min = validateMetric(spo2Min, "Minimum SpO2");
        this.errorRateMax = validateMetric(errorRateMax, "Maximum error rate");
    }

    public void activate() {
        active = true;
    }

    public void deactivate() {
        active = false;
    }

    private Float validateMetric(Float value, String fieldName) {
        if (value == null) {
            return null;
        }
        if (!Float.isFinite(value) || value < 0) {
            throw new IllegalArgumentException(fieldName + " must be a finite non-negative value");
        }
        return value;
    }
}
