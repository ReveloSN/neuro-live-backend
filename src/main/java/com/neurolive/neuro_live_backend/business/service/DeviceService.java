package com.neurolive.neuro_live_backend.business.service;

import com.neurolive.neuro_live_backend.domain.biometric.Device;
import com.neurolive.neuro_live_backend.domain.biometric.DeviceCommand;
import com.neurolive.neuro_live_backend.domain.user.Patient;
import com.neurolive.neuro_live_backend.domain.user.User;
import com.neurolive.neuro_live_backend.infrastructure.integration.DeviceCommandPublisher;
import com.neurolive.neuro_live_backend.repository.DeviceRepository;
import com.neurolive.neuro_live_backend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
    private final ClinicalAccessService clinicalAccessService;
    private final AuditLogService auditLogService;

    public DeviceService(DeviceRepository deviceRepository,
                         UserRepository userRepository,
                         DeviceCommandPublisher deviceCommandPublisher,
                         ClinicalAccessService clinicalAccessService,
                         AuditLogService auditLogService) {
        this.deviceRepository = deviceRepository;
        this.userRepository = userRepository;
        this.deviceCommandPublisher = deviceCommandPublisher;
        this.clinicalAccessService = clinicalAccessService;
        this.auditLogService = auditLogService;
    }

    // Mantiene el contrato legado de registro mientras delega a la misma regla de vinculo unico.
    public Device register(Long patientId, String macAddress, String fallBackConfig) {
        return registerInternal(patientId, macAddress, fallBackConfig);
    }

    // Expone el flujo autorizado de RF05 para asociar un ESP32 a un paciente concreto.
    public Device linkDevice(String requesterEmail,
                             Long patientId,
                             String macAddress,
                             String fallBackConfig,
                             String ipOrigin) {
        User requester = clinicalAccessService.requireDeviceManagementAccess(requesterEmail, patientId);
        Device device = registerInternal(patientId, macAddress, fallBackConfig);
        auditLogService.record(requester.getId(), "LINK_DEVICE", patientId, normalizeIp(ipOrigin));
        return device;
    }

    private Device registerInternal(Long patientId, String macAddress, String fallBackConfig) {
        Patient patient = validatePatientReference(patientId);
        String normalizedMacAddress = normalizeMacAddress(macAddress);
        Optional<Device> existingDevice = deviceRepository.findByMacAddress(normalizedMacAddress);

        if (existingDevice.isPresent()) {
            if (patient.getId().equals(existingDevice.get().getPatientId())) {
                throw new IllegalStateException("Device is already linked to this patient");
            }
            throw new IllegalStateException("Device is already linked to another patient");
        }

        Device device = new Device();
        device.register(patient.getId(), normalizedMacAddress, fallBackConfig);
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

    // Mantiene compatibilidad con el flujo anterior cuando la telemetria no reporta sensorContact.
    public Device registerTelemetry(String macAddress, LocalDateTime telemetryTime) {
        return registerTelemetry(macAddress, telemetryTime, null);
    }

    // Marca presencia de telemetria y conserva si el sensor reporto contacto valido o no.
    public Device registerTelemetry(String macAddress, LocalDateTime telemetryTime, Boolean sensorContact) {
        Device device = findByMacAddress(macAddress);
        device.recordTelemetry(telemetryTime, sensorContact);
        return deviceRepository.save(device);
    }

    // Revisa los dispositivos conectados y devuelve solo los que realmente cruzaron el timeout.
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

    private String normalizeIp(String ipOrigin) {
        return ipOrigin == null || ipOrigin.isBlank() ? "unknown" : ipOrigin.trim();
    }
}
