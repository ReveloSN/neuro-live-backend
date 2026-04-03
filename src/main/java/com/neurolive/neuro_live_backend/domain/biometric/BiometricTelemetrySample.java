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
@Table(name = "biometric_telemetry_samples")
@Getter
@NoArgsConstructor
// Guarda cada muestra biometrica recibida para el analisis posterior.
public class BiometricTelemetrySample {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_id", nullable = false, updatable = false)
    private Long patientId;

    @Column(name = "device_mac", nullable = false, length = 17, updatable = false)
    private String deviceMac;

    @Column(nullable = false, updatable = false)
    private float bpm;

    @Column(nullable = false, updatable = false)
    private float spo2;

    @Column(name = "observed_at", nullable = false, updatable = false)
    private LocalDateTime observedAt;

    private BiometricTelemetrySample(Long patientId, String deviceMac, BiometricData biometricData) {
        this.patientId = validatePatientId(patientId);
        this.deviceMac = normalizeDeviceMac(deviceMac);
        this.bpm = biometricData.bpm();
        this.spo2 = biometricData.spo2();
        this.observedAt = biometricData.timestamp();
    }

    public static BiometricTelemetrySample from(Long patientId, String deviceMac, BiometricData biometricData) {
        if (biometricData == null) {
            throw new IllegalArgumentException("Biometric data is required");
        }
        return new BiometricTelemetrySample(patientId, deviceMac, biometricData);
    }

    public BiometricData toDomain() {
        return new BiometricData(bpm, spo2, observedAt);
    }

    private Long validatePatientId(Long patientId) {
        if (patientId == null || patientId <= 0) {
            throw new IllegalArgumentException("Patient reference must be a positive identifier");
        }
        return patientId;
    }

    private String normalizeDeviceMac(String deviceMac) {
        if (deviceMac == null || deviceMac.isBlank()) {
            throw new IllegalArgumentException("Device MAC address is required");
        }

        return deviceMac.trim()
                .replace('-', ':')
                .toUpperCase();
    }
}
