package com.neurolive.neuro_live_backend.domain.biometric;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Prueba el comportamiento principal de los dispositivos biometricos.
class DeviceTest {

    @Test
    void registerShouldNormalizeMacAddressAndResetConnectionState() {
        Device device = new Device();
        device.updateStatus(true, Instant.parse("2026-03-27T09:00:00Z"));

        device.register(7L, "aa-bb-cc-dd-ee-ff", "  breathing  ");

        assertEquals(7L, device.getPatientId());
        assertEquals("AA:BB:CC:DD:EE:FF", device.getMacAddress());
        assertFalse(device.getIsConnected());
        assertNull(device.getLastConnection());
        assertEquals(Boolean.TRUE, device.getSensorContact());
        assertTrue(device.getLinkedAt() != null);
        assertEquals("breathing", device.getFallBackConfig());
    }

    @Test
    void registerShouldRejectInvalidMacAddress() {
        Device device = new Device();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> device.register(10L, "invalid-mac", null)
        );

        assertEquals("MAC address must use the format AA:BB:CC:DD:EE:FF", exception.getMessage());
    }

    @Test
    void detectDisconnectShouldMarkDeviceAsDisconnectedWhenTimeoutExpires() {
        Device device = new Device();
        device.register(12L, "AA:BB:CC:DD:EE:11", null);
        Instant lastPing = Instant.parse("2026-03-27T10:00:00Z");
        device.updateStatus(true, lastPing);

        boolean disconnected = device.detectDisconnect(Duration.ofMinutes(5), lastPing.plusSeconds(300));

        assertTrue(disconnected);
        assertFalse(device.getIsConnected());
        assertEquals(lastPing, device.getLastConnection());
    }

    @Test
    void recordTelemetryShouldUpdateSensorContactWhenReported() {
        Device device = new Device();
        device.register(13L, "AA:BB:CC:DD:EE:13", null);
        Instant observedAt = Instant.parse("2026-04-09T18:05:00Z");

        device.recordTelemetry(observedAt, Boolean.FALSE);

        assertTrue(device.getIsConnected());
        assertEquals(observedAt, device.getLastConnection());
        assertTrue(device.hasSensorContactIssue());
    }

    @Test
    void sendCommandShouldBuildCommandPayload() throws Exception {
        Device device = new Device();
        device.register(15L, "AA:BB:CC:DD:EE:22", "grounding");
        setId(device, 99L);
        Instant dispatchedAt = Instant.parse("2026-03-27T11:30:00Z");

        DeviceCommand command = device.sendCommand(" START_BREATHING ", dispatchedAt);

        assertEquals(99L, command.deviceId());
        assertEquals("AA:BB:CC:DD:EE:22", command.macAddress());
        assertEquals(15L, command.patientId());
        assertEquals("START_BREATHING", command.command());
        assertEquals(dispatchedAt, command.dispatchedAt());
        assertEquals("grounding", command.fallBackConfig());
    }

    @Test
    void detectDisconnectShouldReturnFalseBeforeTimeoutExpires() {
        Device device = new Device();
        device.register(16L, "AA:BB:CC:DD:EE:33", null);
        Instant lastPing = Instant.parse("2026-03-27T10:00:00Z");
        device.updateStatus(true, lastPing);

        boolean disconnected = device.detectDisconnect(Duration.ofMinutes(5), lastPing.plusSeconds(240));

        assertFalse(disconnected);
        assertTrue(device.getIsConnected());
    }

    @Test
    void registerShouldGenerateProvisioningTokenAutomatically() {
        Device device = new Device();
        device.register(17L, "AA:BB:CC:DD:EE:44", null);

        assertNotNull(device.getProvisioningToken());
        assertFalse(device.getProvisioningToken().isBlank());
    }

    private void setId(Device device, Long id) throws Exception {
        Field field = Device.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(device, id);
    }
}
