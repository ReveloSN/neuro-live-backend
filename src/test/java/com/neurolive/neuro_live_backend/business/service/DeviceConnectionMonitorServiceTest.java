package com.neurolive.neuro_live_backend.business.service;

import com.neurolive.neuro_live_backend.business.patterns.AudioIntervention;
import com.neurolive.neuro_live_backend.business.patterns.BreathingIntervention;
import com.neurolive.neuro_live_backend.business.patterns.CrisisMediator;
import com.neurolive.neuro_live_backend.business.patterns.LightIntervention;
import com.neurolive.neuro_live_backend.business.patterns.PatientStateObserver;
import com.neurolive.neuro_live_backend.business.patterns.PatientStateUpdate;
import com.neurolive.neuro_live_backend.business.patterns.UIIntervention;
import com.neurolive.neuro_live_backend.domain.biometric.Device;
import com.neurolive.neuro_live_backend.infrastructure.config.TelemetryMonitoringProperties;
import com.neurolive.neuro_live_backend.repository.DeviceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceConnectionMonitorServiceTest {

    @Mock
    private DeviceService deviceService;

    @Mock
    private DeviceRepository deviceRepository;

    @Test
    void shouldKeepDeviceConnectedWhenHeartbeatIsRecent() {
        LocalDateTime lastHeartbeat = LocalDateTime.of(2026, 4, 2, 11, 0);
        Device device = buildConnectedDevice(91L, "AA:BB:CC:DD:EE:91", lastHeartbeat);
        RecordingObserver observer = new RecordingObserver();
        DeviceConnectionMonitorService monitorService = buildMonitorService(device, observer, 5L);

        List<Device> disconnectedDevices = monitorService.scanForDisconnects(lastHeartbeat.plusSeconds(4));

        assertTrue(disconnectedDevices.isEmpty());
        assertTrue(device.getIsConnected());
        assertTrue(observer.updates().isEmpty());
    }

    @Test
    void shouldMarkDeviceDisconnectedWhenHeartbeatIsStale() {
        LocalDateTime lastHeartbeat = LocalDateTime.of(2026, 4, 2, 11, 5);
        Device device = buildConnectedDevice(92L, "AA:BB:CC:DD:EE:92", lastHeartbeat);
        RecordingObserver observer = new RecordingObserver();
        DeviceConnectionMonitorService monitorService = buildMonitorService(device, observer, 5L);

        List<Device> disconnectedDevices = monitorService.scanForDisconnects(lastHeartbeat.plusSeconds(6));

        assertEquals(1, disconnectedDevices.size());
        assertFalse(device.getIsConnected());
        assertEquals(1, observer.updates().size());
        PatientStateUpdate update = observer.updates().getFirst();
        assertTrue(update.isDisconnectAlert());
        assertTrue(update.shouldNotifyCaregiver());
        assertFalse(update.shouldNotifyDoctor());
        assertEquals(Boolean.FALSE, update.deviceConnected());
        assertEquals(Boolean.TRUE, update.sensorContact());
        assertNull(update.emotionalState());
    }

    @Test
    void shouldNotEmitDuplicateDisconnectAlertsOnRepeatedScans() {
        LocalDateTime lastHeartbeat = LocalDateTime.of(2026, 4, 2, 11, 10);
        Device device = buildConnectedDevice(93L, "AA:BB:CC:DD:EE:93", lastHeartbeat);
        RecordingObserver observer = new RecordingObserver();
        DeviceConnectionMonitorService monitorService = buildMonitorService(device, observer, 5L);

        monitorService.scanForDisconnects(lastHeartbeat.plusSeconds(6));
        monitorService.scanForDisconnects(lastHeartbeat.plusSeconds(7));

        assertEquals(1, observer.updates().size());
    }

    @Test
    void shouldAllowReconnectionThroughLaterTelemetryUpdates() {
        LocalDateTime lastHeartbeat = LocalDateTime.of(2026, 4, 2, 11, 15);
        Device device = buildConnectedDevice(94L, "AA:BB:CC:DD:EE:94", lastHeartbeat);
        RecordingObserver observer = new RecordingObserver();
        DeviceConnectionMonitorService monitorService = buildMonitorService(device, observer, 5L);

        monitorService.scanForDisconnects(lastHeartbeat.plusSeconds(6));
        // Permite recuperar el estado cuando vuelve la telemetria
        device.updateStatus(true, lastHeartbeat.plusSeconds(8));
        monitorService.scanForDisconnects(lastHeartbeat.plusSeconds(9));
        monitorService.scanForDisconnects(lastHeartbeat.plusSeconds(14));

        assertEquals(2, observer.updates().size());
        assertFalse(device.getIsConnected());
    }

    @Test
    void shouldRespectConfiguredTimeoutValues() {
        LocalDateTime lastHeartbeat = LocalDateTime.of(2026, 4, 2, 11, 20);
        Device device = buildConnectedDevice(95L, "AA:BB:CC:DD:EE:95", lastHeartbeat);
        RecordingObserver observer = new RecordingObserver();
        DeviceConnectionMonitorService monitorService = buildMonitorService(device, observer, 10L);

        monitorService.scanForDisconnects(lastHeartbeat.plusSeconds(6));
        assertTrue(device.getIsConnected());

        monitorService.scanForDisconnects(lastHeartbeat.plusSeconds(11));
        assertFalse(device.getIsConnected());
        assertEquals(1, observer.updates().size());
    }

    @Test
    void shouldUseGracePeriodsWhenTheyProduceALongerTimeout() {
        LocalDateTime lastHeartbeat = LocalDateTime.of(2026, 4, 2, 11, 25);
        Device device = buildConnectedDevice(96L, "AA:BB:CC:DD:EE:96", lastHeartbeat);
        RecordingObserver observer = new RecordingObserver();
        DeviceConnectionMonitorService monitorService = buildMonitorService(device, observer, 5L, 4L, 2L);

        monitorService.scanForDisconnects(lastHeartbeat.plusSeconds(6));
        assertTrue(device.getIsConnected());

        monitorService.scanForDisconnects(lastHeartbeat.plusSeconds(9));
        assertFalse(device.getIsConnected());
        assertEquals(1, observer.updates().size());
    }

    private DeviceConnectionMonitorService buildMonitorService(Device device,
                                                              RecordingObserver observer,
                                                              long timeoutSeconds) {
        return buildMonitorService(device, observer, timeoutSeconds, 1L, 1L);
    }

    private DeviceConnectionMonitorService buildMonitorService(Device device,
                                                              RecordingObserver observer,
                                                              long timeoutSeconds,
                                                              long expectedIntervalSeconds,
                                                              long gracePeriods) {
        when(deviceService.detectDisconnect(anyLong(), any(LocalDateTime.class))).thenAnswer(invocation -> {
            long configuredTimeout = invocation.getArgument(0);
            LocalDateTime referenceTime = invocation.getArgument(1);
            return device.detectDisconnect(Duration.ofSeconds(configuredTimeout), referenceTime)
                    ? List.of(device)
                    : List.of();
        });

        TelemetryMonitoringProperties properties = new TelemetryMonitoringProperties();
        properties.setDisconnectTimeoutSeconds(timeoutSeconds);
        properties.setDisconnectCheckIntervalSeconds(1L);
        properties.setExpectedTelemetryIntervalSeconds(expectedIntervalSeconds);
        properties.setDisconnectGracePeriods(gracePeriods);

        return new DeviceConnectionMonitorService(
                deviceService,
                new CrisisMediator(
                        List.of(
                                new UIIntervention(),
                                new BreathingIntervention(),
                                new LightIntervention(),
                                new AudioIntervention()
                        ),
                        List.of(observer)
                ),
                properties,
                deviceRepository
        );
    }

    private Device buildConnectedDevice(Long patientId, String macAddress, LocalDateTime lastHeartbeat) {
        Device device = new Device();
        device.register(patientId, macAddress, null);
        device.updateStatus(true, lastHeartbeat);
        return device;
    }

    private static final class RecordingObserver implements PatientStateObserver {

        private final List<PatientStateUpdate> updates = new ArrayList<>();

        @Override
        public void onPatientStateChanged(PatientStateUpdate update) {
            updates.add(update);
        }

        private List<PatientStateUpdate> updates() {
            return updates;
        }
    }
}
