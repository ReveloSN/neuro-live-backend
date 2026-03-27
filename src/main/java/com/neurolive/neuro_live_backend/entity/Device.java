package com.neurolive.neuro_live_backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;

@Entity
@Table(name = "devices")
@Getter
@NoArgsConstructor
public class Device {

    private static final java.util.regex.Pattern MAC_ADDRESS_PATTERN = java.util.regex.Pattern.compile(
            "^(?:[0-9A-F]{2}:){5}[0-9A-F]{2}$"
    );

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Pattern(regexp = "^(?:[0-9A-F]{2}:){5}[0-9A-F]{2}$", message = "MAC address must use the format AA:BB:CC:DD:EE:FF")
    @Column(name = "mac_address", nullable = false, unique = true, length = 17)
    private String macAddress;

    @NotNull
    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @NotNull
    @Column(name = "is_connected", nullable = false)
    private Boolean isConnected = false;

    @Column(name = "last_connection")
    private LocalDateTime lastConnection;

    @Column(name = "fall_back_config", length = 2048)
    private String fallBackConfig;

    public void register(Long patientId, String macAddress, String fallBackConfig) {
        this.patientId = validatePatientId(patientId);
        this.macAddress = normalizeMacAddress(macAddress);
        this.fallBackConfig = normalizeFallbackConfig(fallBackConfig);
        this.isConnected = false;
        this.lastConnection = null;
    }

    public boolean detectDisconnect(Duration timeout, LocalDateTime referenceTime) {
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("Timeout must be greater than zero");
        }
        if (referenceTime == null) {
            throw new IllegalArgumentException("Reference time is required");
        }
        if (!Boolean.TRUE.equals(isConnected) || lastConnection == null) {
            return false;
        }
        if (lastConnection.plus(timeout).isAfter(referenceTime)) {
            return false;
        }

        isConnected = false;
        return true;
    }

    public DeviceCommand sendCommand(String command, LocalDateTime dispatchedAt) {
        if (id == null) {
            throw new IllegalStateException("Device must be persisted before sending commands");
        }
        if (macAddress == null || patientId == null) {
            throw new IllegalStateException("Device must be registered before sending commands");
        }
        if (command == null || command.isBlank()) {
            throw new IllegalArgumentException("Command is required");
        }
        if (dispatchedAt == null) {
            throw new IllegalArgumentException("Dispatch time is required");
        }

        return new DeviceCommand(
                id,
                macAddress,
                patientId,
                command.trim(),
                dispatchedAt,
                fallBackConfig
        );
    }

    public void updateStatus(boolean connected, LocalDateTime statusTime) {
        if (statusTime == null) {
            throw new IllegalArgumentException("Status time is required");
        }

        isConnected = connected;
        if (connected) {
            lastConnection = statusTime;
        }
    }

    private Long validatePatientId(Long patientId) {
        if (patientId == null || patientId <= 0) {
            throw new IllegalArgumentException("Patient reference must be a positive identifier");
        }
        return patientId;
    }

    private String normalizeMacAddress(String macAddress) {
        if (macAddress == null || macAddress.isBlank()) {
            throw new IllegalArgumentException("MAC address is required");
        }

        String normalized = macAddress.trim()
                .replace('-', ':')
                .toUpperCase();

        if (!MAC_ADDRESS_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("MAC address must use the format AA:BB:CC:DD:EE:FF");
        }

        return normalized;
    }

    private String normalizeFallbackConfig(String fallBackConfig) {
        if (fallBackConfig == null) {
            return null;
        }

        String normalized = fallBackConfig.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
