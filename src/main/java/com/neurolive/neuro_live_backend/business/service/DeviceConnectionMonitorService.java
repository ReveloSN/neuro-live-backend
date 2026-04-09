package com.neurolive.neuro_live_backend.business.service;

import com.neurolive.neuro_live_backend.business.patterns.CrisisMediator;
import com.neurolive.neuro_live_backend.business.patterns.PatientStateUpdate;
import com.neurolive.neuro_live_backend.domain.biometric.Device;
import com.neurolive.neuro_live_backend.infrastructure.config.TelemetryMonitoringProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
// Detecta dispositivos sin telemetria reciente
public class DeviceConnectionMonitorService {

    private final DeviceService deviceService;
    private final CrisisMediator crisisMediator;
    private final TelemetryMonitoringProperties telemetryMonitoringProperties;

    public DeviceConnectionMonitorService(DeviceService deviceService,
                                          CrisisMediator crisisMediator,
                                          TelemetryMonitoringProperties telemetryMonitoringProperties) {
        this.deviceService = deviceService;
        this.crisisMediator = crisisMediator;
        this.telemetryMonitoringProperties = telemetryMonitoringProperties;
    }

    @Scheduled(
            initialDelayString = "${telemetry.disconnect-check-interval-seconds:1}",
            fixedDelayString = "${telemetry.disconnect-check-interval-seconds:1}",
            timeUnit = TimeUnit.SECONDS
    )
    public void scanForDisconnects() {
        scanForDisconnects(LocalDateTime.now());
    }

    // Ejecuta la deteccion periodica usando un timeout efectivo que combina timeout y grace period.
    public List<Device> scanForDisconnects(LocalDateTime referenceTime) {
        if (referenceTime == null) {
            throw new IllegalArgumentException("Reference time is required");
        }

        long timeoutSeconds = resolveDisconnectTimeoutSeconds();
        // Marca el dispositivo como desconectado al superar el tiempo limite
        List<Device> disconnectedDevices = deviceService.detectDisconnect(timeoutSeconds, referenceTime);

        // Evita duplicar alertas de desconexion
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

    // Permite suavizar falsas alarmas usando el mayor valor entre timeout fijo e intervalo esperado por gracia.
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

    private long validatePositiveValue(long value, String fieldName) {
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be greater than zero");
        }
        return value;
    }
}
