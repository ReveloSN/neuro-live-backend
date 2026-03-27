package com.neurolive.neuro_live_backend.service;

import com.neurolive.neuro_live_backend.entity.Device;
import com.neurolive.neuro_live_backend.entity.DeviceCommand;
import com.neurolive.neuro_live_backend.entity.Role;
import com.neurolive.neuro_live_backend.entity.User;
import com.neurolive.neuro_live_backend.enums.RoleName;
import com.neurolive.neuro_live_backend.repository.DeviceRepository;
import com.neurolive.neuro_live_backend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceServiceTest {

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DeviceCommandPublisher deviceCommandPublisher;

    @InjectMocks
    private DeviceService deviceService;

    @Test
    void registerShouldSaveDeviceForPatient() {
        User patient = buildPatient(3L);
        when(userRepository.findById(3L)).thenReturn(Optional.of(patient));
        when(deviceRepository.existsByMacAddress("AA:BB:CC:DD:EE:FF")).thenReturn(false);
        when(deviceRepository.save(any(Device.class))).thenAnswer(invocation -> {
            Device device = invocation.getArgument(0);
            setId(device, 20L);
            return device;
        });

        Device registeredDevice = deviceService.register(3L, "aa-bb-cc-dd-ee-ff", "calm-mode");

        assertEquals(20L, registeredDevice.getId());
        assertEquals("AA:BB:CC:DD:EE:FF", registeredDevice.getMacAddress());
        assertEquals(3L, registeredDevice.getPatientId());
        assertEquals("calm-mode", registeredDevice.getFallBackConfig());
        assertEquals(Boolean.FALSE, registeredDevice.getIsConnected());
    }

    @Test
    void registerShouldRejectNonPatientReference() {
        User caregiver = buildUser(4L, RoleName.CUIDADOR);
        when(userRepository.findById(4L)).thenReturn(Optional.of(caregiver));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> deviceService.register(4L, "AA:BB:CC:DD:EE:FF", null)
        );

        assertEquals("Referenced user is not a patient", exception.getMessage());
        verify(deviceRepository, never()).save(any(Device.class));
    }

    @Test
    void registerTelemetryShouldMarkDeviceAsConnected() {
        Device device = new Device();
        device.register(6L, "AA:BB:CC:DD:EE:01", null);
        when(deviceRepository.findByMacAddress("AA:BB:CC:DD:EE:01")).thenReturn(Optional.of(device));
        when(deviceRepository.save(device)).thenReturn(device);
        LocalDateTime telemetryTime = LocalDateTime.of(2026, 3, 27, 12, 0);

        Device updatedDevice = deviceService.registerTelemetry("aa-bb-cc-dd-ee-01", telemetryTime);

        assertTrue(updatedDevice.getIsConnected());
        assertEquals(telemetryTime, updatedDevice.getLastConnection());
    }

    @Test
    void detectDisconnectShouldSaveOnlyDisconnectedDevices() {
        Device staleDevice = new Device();
        staleDevice.register(8L, "AA:BB:CC:DD:EE:10", null);
        staleDevice.updateStatus(true, LocalDateTime.of(2026, 3, 27, 9, 0));

        Device freshDevice = new Device();
        freshDevice.register(8L, "AA:BB:CC:DD:EE:11", null);
        freshDevice.updateStatus(true, LocalDateTime.of(2026, 3, 27, 9, 59));

        when(deviceRepository.findAllByIsConnectedTrue()).thenReturn(List.of(staleDevice, freshDevice));
        when(deviceRepository.save(staleDevice)).thenReturn(staleDevice);
        LocalDateTime referenceTime = LocalDateTime.of(2026, 3, 27, 10, 0);

        List<Device> disconnectedDevices = deviceService.detectDisconnect(90L, referenceTime);

        assertEquals(1, disconnectedDevices.size());
        assertSame(staleDevice, disconnectedDevices.getFirst());
        assertEquals(Boolean.FALSE, staleDevice.getIsConnected());
        assertEquals(Boolean.TRUE, freshDevice.getIsConnected());
        verify(deviceRepository).save(staleDevice);
        verify(deviceRepository, never()).save(freshDevice);
    }

    @Test
    void sendCommandShouldPublishInterventionCommand() {
        Device device = new Device();
        device.register(9L, "AA:BB:CC:DD:EE:20", "fallback");
        setId(device, 55L);
        when(deviceRepository.findById(55L)).thenReturn(Optional.of(device));
        LocalDateTime dispatchedAt = LocalDateTime.of(2026, 3, 27, 14, 30);

        DeviceCommand command = deviceService.sendCommand(55L, "START_GROUNDING", dispatchedAt);

        ArgumentCaptor<DeviceCommand> commandCaptor = ArgumentCaptor.forClass(DeviceCommand.class);
        verify(deviceCommandPublisher).publish(commandCaptor.capture());
        assertEquals(command, commandCaptor.getValue());
        assertEquals("START_GROUNDING", command.command());
        assertEquals("AA:BB:CC:DD:EE:20", command.macAddress());
    }

    @Test
    void findByMacAddressShouldFailWhenDeviceDoesNotExist() {
        when(deviceRepository.findByMacAddress("AA:BB:CC:DD:EE:99")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> deviceService.findByMacAddress("AA:BB:CC:DD:EE:99"));
    }

    private User buildPatient(Long id) {
        return buildUser(id, RoleName.PACIENTE);
    }

    private User buildUser(Long id, RoleName roleName) {
        Role role = new Role();
        role.setName(roleName);

        User user = new User();
        user.setId(id);
        user.setRole(role);
        user.setActive(true);
        user.setEmail("user" + id + "@neurolive.test");
        user.setName("User " + id);
        user.setPassword("secret");
        return user;
    }

    private void setId(Device device, Long id) {
        try {
            Field field = Device.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(device, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to set device id for test setup", exception);
        }
    }
}
