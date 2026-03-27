package com.neurolive.neuro_live_backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.ToDoubleFunction;

@Entity
@Table(name = "baselines")
@Getter
@NoArgsConstructor
public class BaseLine {

    static final Duration BASELINE_WINDOW = Duration.ofMinutes(5);
    private static final int MINIMUM_SAMPLE_COUNT = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "patient_id", nullable = false, unique = true)
    private Long patientId;

    @Column(name = "avg_bpm", nullable = false)
    private float avgBpm;

    @Column(name = "avg_spo2", nullable = false)
    private float avgSpo2;

    @Column(name = "calculated_at")
    private LocalDateTime calculatedAt;

    public BaseLine(Long patientId) {
        this.patientId = validatePatientId(patientId);
    }

    public BaseLine calculate(Collection<BiometricSample> biometricSamples) {
        return calculate(biometricSamples, sample -> 1.0d);
    }

    public boolean isReady() {
        return patientId != null
                && patientId > 0
                && calculatedAt != null
                && isValidCalculatedMetric(avgBpm)
                && isValidCalculatedMetric(avgSpo2);
    }

    public BaseLine calculate(Collection<BiometricSample> biometricSamples,
                            ToDoubleFunction<BiometricSample> weightFunction) {
        if (patientId == null) {
            throw new IllegalStateException("Patient reference must be defined before baseline calculation");
        }
        if (weightFunction == null) {
            throw new IllegalArgumentException("Weight function is required");
        }

        List<BiometricSample> samples = normalizeSamples(biometricSamples);
        List<BiometricSample> baselineWindowSamples = extractBaselineWindow(samples);

        if (!hasEnoughData(baselineWindowSamples)) {
            return this;
        }

        WeightedAverage averages = calculateWeightedAverage(baselineWindowSamples, weightFunction);
        return applyCalculation(
                (float) averages.avgBpm(),
                (float) averages.avgSpo2(),
                baselineWindowSamples.getLast().timestamp()
        );
    }

    BaseLine applyCalculation(float avgBpm, float avgSpo2, LocalDateTime calculatedAt) {
        validateCalculatedMetric(avgBpm, "Average BPM");
        validateCalculatedMetric(avgSpo2, "Average SpO2");
        if (calculatedAt == null) {
            throw new IllegalArgumentException("Calculation timestamp is required");
        }

        this.avgBpm = avgBpm;
        this.avgSpo2 = avgSpo2;
        this.calculatedAt = calculatedAt;
        return this;
    }

    private Long validatePatientId(Long patientId) {
        if (patientId == null || patientId <= 0) {
            throw new IllegalArgumentException("Patient reference must be a positive identifier");
        }
        return patientId;
    }

    private List<BiometricSample> normalizeSamples(Collection<BiometricSample> biometricSamples) {
        if (biometricSamples == null) {
            throw new IllegalArgumentException("Biometric samples are required");
        }
        if (biometricSamples.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Biometric samples cannot contain null values");
        }

        return biometricSamples.stream()
                .sorted(Comparator.comparing(BiometricSample::timestamp))
                .toList();
    }

    private List<BiometricSample> extractBaselineWindow(List<BiometricSample> samples) {
        if (samples.isEmpty()) {
            return List.of();
        }

        LocalDateTime baselineWindowEnd = samples.getFirst().timestamp().plus(BASELINE_WINDOW);

        return samples.stream()
                .filter(sample -> !sample.timestamp().isAfter(baselineWindowEnd))
                .toList();
    }

    private boolean hasEnoughData(List<BiometricSample> baselineWindowSamples) {
        if (baselineWindowSamples.size() < MINIMUM_SAMPLE_COUNT) {
            return false;
        }

        LocalDateTime baselineWindowEnd = baselineWindowSamples.getFirst().timestamp().plus(BASELINE_WINDOW);
        return !baselineWindowSamples.getLast().timestamp().isBefore(baselineWindowEnd);
    }

    private WeightedAverage calculateWeightedAverage(List<BiometricSample> biometricSamples,
                                                    ToDoubleFunction<BiometricSample> weightFunction) {
        double totalWeight = 0.0d;
        double weightedBpm = 0.0d;
        double weightedSpo2 = 0.0d;

        for (BiometricSample biometricSample : biometricSamples) {
            double weight = weightFunction.applyAsDouble(biometricSample);

            if (!Double.isFinite(weight) || weight <= 0.0d) {
                throw new IllegalArgumentException("Sample weight must be a finite positive value");
            }

            totalWeight += weight;
            weightedBpm += biometricSample.bpm() * weight;
            weightedSpo2 += biometricSample.spo2() * weight;
        }

        if (totalWeight <= 0.0d) {
            throw new IllegalStateException("Total weight must be greater than zero");
        }

        return new WeightedAverage(weightedBpm / totalWeight, weightedSpo2 / totalWeight);
    }

    private boolean isValidCalculatedMetric(float value) {
        return Float.isFinite(value) && value >= 0;
    }

    private void validateCalculatedMetric(float value, String fieldName) {
        if (!Float.isFinite(value) || value < 0) {
            throw new IllegalArgumentException(fieldName + " must be a finite non-negative value");
        }
    }

    private record WeightedAverage(double avgBpm, double avgSpo2) {
    }
}
