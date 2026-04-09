package com.neurolive.neuro_live_backend.domain.biometric;

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
// Almacena la linea base biometrica calculada para un paciente.
public class BaseLine {

    static final Duration DEFAULT_BASELINE_WINDOW = Duration.ofMinutes(5);
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

    public BaseLine calculate(Collection<BiometricData> biometricSamples) {
        return calculate(biometricSamples, DEFAULT_BASELINE_WINDOW, sample -> 1.0d);
    }

    public BaseLine calculate(Collection<BiometricData> biometricSamples, Duration baselineWindow) {
        return calculate(biometricSamples, baselineWindow, sample -> 1.0d);
    }

    public boolean isReady() {
        return patientId != null
                && patientId > 0
                && calculatedAt != null
                && isValidCalculatedMetric(avgBpm)
                && isValidCalculatedMetric(avgSpo2);
    }

    public BaseLine calculate(Collection<BiometricData> biometricSamples,
                              ToDoubleFunction<BiometricData> weightFunction) {
        return calculate(biometricSamples, DEFAULT_BASELINE_WINDOW, weightFunction);
    }

    public BaseLine calculate(Collection<BiometricData> biometricSamples,
                              Duration baselineWindow,
                              ToDoubleFunction<BiometricData> weightFunction) {
        if (patientId == null) {
            throw new IllegalStateException("Patient reference must be defined before baseline calculation");
        }
        if (baselineWindow == null || baselineWindow.isNegative() || baselineWindow.isZero()) {
            throw new IllegalArgumentException("Baseline window must be greater than zero");
        }
        if (weightFunction == null) {
            throw new IllegalArgumentException("Weight function is required");
        }

        List<BiometricData> samples = normalizeSamples(biometricSamples);
        List<BiometricData> baselineWindowSamples = extractBaselineWindow(samples, baselineWindow);

        if (!hasEnoughData(baselineWindowSamples, baselineWindow)) {
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

    private List<BiometricData> normalizeSamples(Collection<BiometricData> biometricSamples) {
        if (biometricSamples == null) {
            throw new IllegalArgumentException("Biometric samples are required");
        }
        if (biometricSamples.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Biometric samples cannot contain null values");
        }

        return biometricSamples.stream()
                .sorted(Comparator.comparing(BiometricData::timestamp))
                .toList();
    }

    private List<BiometricData> extractBaselineWindow(List<BiometricData> samples, Duration baselineWindow) {
        if (samples.isEmpty()) {
            return List.of();
        }

        LocalDateTime baselineWindowEnd = samples.getFirst().timestamp().plus(baselineWindow);

        return samples.stream()
                .filter(sample -> !sample.timestamp().isAfter(baselineWindowEnd))
                .toList();
    }

    private boolean hasEnoughData(List<BiometricData> baselineWindowSamples, Duration baselineWindow) {
        if (baselineWindowSamples.size() < MINIMUM_SAMPLE_COUNT) {
            return false;
        }

        LocalDateTime baselineWindowEnd = baselineWindowSamples.getFirst().timestamp().plus(baselineWindow);
        return !baselineWindowSamples.getLast().timestamp().isBefore(baselineWindowEnd);
    }

    private WeightedAverage calculateWeightedAverage(List<BiometricData> biometricSamples,
                                                     ToDoubleFunction<BiometricData> weightFunction) {
        double totalWeight = 0.0d;
        double weightedBpm = 0.0d;
        double weightedSpo2 = 0.0d;

        for (BiometricData biometricSample : biometricSamples) {
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
