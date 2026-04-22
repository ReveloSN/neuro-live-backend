package com.neurolive.neuro_live_backend.business.service;

import com.neurolive.neuro_live_backend.business.patterns.CrisisMediator;
import com.neurolive.neuro_live_backend.business.patterns.PatientStateUpdate;
import com.neurolive.neuro_live_backend.domain.biometric.Device;
import com.neurolive.neuro_live_backend.infrastructure.config.TelemetryMonitoringProperties;
import com.neurolive.neuro_live_backend.repository.DeviceRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

// Detecta dispositivos sin telemetria reciente y procesa notificaciones del WS Service.
@Service
public class DeviceConnectionMonitorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeviceConnectionMonitorService.class);

    private final DeviceService deviceService;
    private final CrisisMediator crisisMediator;
    private final TelemetryMonitoringProperties telemetryMonitoringProperties;
    private final DeviceRepository deviceRepository;

    // Recibe las dependencias necesarias para monitorear conectividad.
    public DeviceConnectionMonitorService(DeviceService deviceService,
                                          CrisisMediator crisisMediator,
                                          TelemetryMonitoringProperties telemetryMonitoringProperties,
                                          DeviceRepository deviceRepository) {
        this.deviceService = deviceService;
        this.crisisMediator = crisisMediator;
        this.telemetryMonitoringProperties = telemetryMonitoringProperties;
        this.deviceRepository = deviceRepository;
    }

    // Ejecuta el escaneo periodico de desconexiones locales.
    @Scheduled(
            initialDelayString = "${telemetry.disconnect-check-interval-seconds:1}",
            fixedDelayString = "${telemetry.disconnect-check-interval-seconds:1}",
            timeUnit = TimeUnit.SECONDS
    )
    public void scanForDisconnects() {
        scanForDisconnects(LocalDateTime.now());
    }

    // Ejecuta la deteccion periodica usando el timeout efectivo.
    public List<Device> scanForDisconnects(LocalDateTime referenceTime) {
        if (referenceTime == null) {
            throw new IllegalArgumentException("Reference time is required");
        }

        long timeoutSeconds = resolveDisconnectTimeoutSeconds();
        List<Device> disconnectedDevices = deviceService.detectDisconnect(timeoutSeconds, referenceTime);

        disconnectedDevices.forEach(device -> crisisMediator.publishUpdate(
                PatientStateUpdate.caregiverDeviceStatus(
                        device.getPatientId(),
                        referenceTime,
                        Boolean.FALSE,
                        device.getSensorContact()
                )
        ));

        return disconnectedDevices;
    }

    // Procesa una notificacion explicita de desconexion desde el WS Service.
    public void handleDeviceDisconnected(String deviceId, String reason) {
        LOGGER.warn("Device disconnected notification received: deviceId={}, reason={}", deviceId, reason);
        Device device = findKnownDevice(deviceId);
        if (device == null) {
            return;
        }

        Device updatedDevice = deviceService.updateStatus(device.getId(), false, LocalDateTime.now());
        crisisMediator.publishUpdate(
                PatientStateUpdate.caregiverDeviceStatus(
                        updatedDevice.getPatientId(),
                        LocalDateTime.now(),
                        Boolean.FALSE,
                        updatedDevice.getSensorContact()
                )
        );
    }

    // Procesa una notificacion explicita de conexion desde el WS Service.
    public void handleDeviceConnected(String deviceId) {
        LOGGER.info("Device connected notification received: deviceId={}", deviceId);
        Device device = findKnownDevice(deviceId);
        if (device == null) {
            return;
        }

        Device updatedDevice = deviceService.updateStatus(device.getId(), true, LocalDateTime.now());
        crisisMediator.publishUpdate(
                PatientStateUpdate.caregiverDeviceStatus(
                        updatedDevice.getPatientId(),
                        LocalDateTime.now(),
                        Boolean.TRUE,
                        updatedDevice.getSensorContact()
                )
        );
    }

    // Valida si el dispositivo existe para autenticacion remota basica.
    public boolean isValidDeviceToken(String deviceId, String token) {
        if (deviceId == null || deviceId.isBlank()) {
            return false;
        }
        return deviceRepository.existsByMacAddress(normalizeMacAddress(deviceId));
    }

    // Calcula el timeout final usando la configuracion de gracia.
    private long resolveDisconnectTimeoutSeconds() {
        long timeoutSeconds = validatePositiveValue(
                telemetryMonitoringProperties.getDisconnectTimeoutSeconds(),
                "Disconnect timeout"
        );
        long expectedIntervalSeconds = validatePositiveValue(
                telemetryMonitoringProperties.getExpectedTelemetryIntervalSeconds(),
                "Expected telemetry interval"
        );
        long gracePeriods = validatePositiveValue(
                telemetryMonitoringProperties.getDisconnectGracePeriods(),
                "Disconnect grace periods"
        );
        return Math.max(timeoutSeconds, Math.multiplyExact(expectedIntervalSeconds, gracePeriods));
    }

    // Busca un dispositivo conocido sin romper el flujo interno.
    private Device findKnownDevice(String deviceId) {
        try {
            return deviceService.findByMacAddress(deviceId);
        } catch (EntityNotFoundException | IllegalArgumentException exception) {
            LOGGER.warn("Device connectivity notification ignored for unknown deviceId={}", deviceId);
            return null;
        }
    }

    // Normaliza el MAC recibido desde el servicio realtime.
    private String normalizeMacAddress(String deviceId) {
        return deviceId.trim()
                .replace('-', ':')
                .toUpperCase();
    }

    // Valida que la configuracion tenga un valor positivo.
    private long validatePositiveValue(long value, String fieldName) {
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be greater than zero");
        }
        return value;
    }
}

