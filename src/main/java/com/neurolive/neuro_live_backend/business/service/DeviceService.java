package com.neurolive.neuro_live_backend.business.service;

import com.neurolive.neuro_live_backend.domain.biometric.Device;
import com.neurolive.neuro_live_backend.domain.biometric.DeviceCommand;
import com.neurolive.neuro_live_backend.domain.user.Patient;
import com.neurolive.neuro_live_backend.domain.user.User;
import com.neurolive.neuro_live_backend.infrastructure.mqtt.DeviceCommandPublisher;
import com.neurolive.neuro_live_backend.repository.DeviceRepository;
import com.neurolive.neuro_live_backend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
// Coordina el registro, estado y comandos de los dispositivos biometricos.
public class DeviceService {

    private static final java.util.regex.Pattern MAC_ADDRESS_PATTERN = java.util.regex.Pattern.compile(
            "^(?:[0-9A-F]{2}:){5}[0-9A-F]{2}$"
    );

    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;
    private final DeviceCommandPublisher deviceCommandPublisher;

    public DeviceService(DeviceRepository deviceRepository,
                        UserRepository userRepository,
                        DeviceCommandPublisher deviceCommandPublisher) {
        this.deviceRepository = deviceRepository;
        this.userRepository = userRepository;
        this.deviceCommandPublisher = deviceCommandPublisher;
    }

    public Device register(Long patientId, String macAddress, String fallBackConfig) {
        Patient patient = validatePatientReference(patientId);

        Device device = new Device();
        device.register(patient.getId(), macAddress, fallBackConfig);

        if (deviceRepository.existsByMacAddress(device.getMacAddress())) {
            throw new IllegalArgumentException("MAC address is already registered");
        }

        return deviceRepository.save(device);
    }

    @Transactional(readOnly = true)
    public Device findByMacAddress(String macAddress) {
        String normalizedMacAddress = normalizeMacAddress(macAddress);
        return deviceRepository.findByMacAddress(normalizedMacAddress)
                .orElseThrow(() -> new EntityNotFoundException("Device not found for MAC address " + normalizedMacAddress));
    }

    @Transactional(readOnly = true)
    public List<Device> findByPatientId(Long patientId) {
        validatePatientReference(patientId);
        return deviceRepository.findAllByPatientId(patientId);
    }

    public Device updateStatus(Long deviceId, boolean connected, LocalDateTime statusTime) {
        Device device = getDevice(deviceId);
        device.updateStatus(connected, statusTime);
        return deviceRepository.save(device);
    }

    public Device registerTelemetry(String macAddress, LocalDateTime telemetryTime) {
        Device device = findByMacAddress(macAddress);
        device.updateStatus(true, telemetryTime);
        return deviceRepository.save(device);
    }

    public List<Device> detectDisconnect(Long timeoutSeconds, LocalDateTime referenceTime) {
        if (timeoutSeconds == null || timeoutSeconds <= 0) {
            throw new IllegalArgumentException("Timeout must be greater than zero");
        }
        if (referenceTime == null) {
            throw new IllegalArgumentException("Reference time is required");
        }

        Duration timeout = Duration.ofSeconds(timeoutSeconds);

        return deviceRepository.findAllByIsConnectedTrue().stream()
                .filter(device -> device.detectDisconnect(timeout, referenceTime))
                .map(deviceRepository::save)
                .toList();
    }

    public DeviceCommand sendCommand(Long deviceId, String command, LocalDateTime dispatchedAt) {
        Device device = getDevice(deviceId);
        DeviceCommand deviceCommand = device.sendCommand(command, dispatchedAt);
        deviceCommandPublisher.publish(deviceCommand);
        return deviceCommand;
    }

    private Device getDevice(Long deviceId) {
        if (deviceId == null || deviceId <= 0) {
            throw new IllegalArgumentException("Device identifier must be positive");
        }

        return deviceRepository.findById(deviceId)
                .orElseThrow(() -> new EntityNotFoundException("Device not found with id " + deviceId));
    }

    private Patient validatePatientReference(Long patientId) {
        if (patientId == null || patientId <= 0) {
            throw new IllegalArgumentException("Patient reference must be a positive identifier");
        }

        User patient = userRepository.findById(patientId)
                .orElseThrow(() -> new EntityNotFoundException("Patient not found with id " + patientId));

        if (!(patient instanceof Patient typedPatient)) {
            throw new IllegalArgumentException("Referenced user is not a patient");
        }

        return typedPatient;
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
}
