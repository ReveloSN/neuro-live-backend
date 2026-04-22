package com.neurolive.neuro_live_backend.business.service;

import com.neurolive.neuro_live_backend.domain.biometric.Device;
import com.neurolive.neuro_live_backend.domain.biometric.DeviceCommand;
import com.neurolive.neuro_live_backend.domain.user.Caregiver;
import com.neurolive.neuro_live_backend.domain.user.Patient;
import com.neurolive.neuro_live_backend.domain.user.User;
import com.neurolive.neuro_live_backend.infrastructure.integration.DeviceCommandPublisher;
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
// Prueba la coordinacion del servicio encargado de los dispositivos.
class DeviceServiceTest {

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DeviceCommandPublisher deviceCommandPublisher;

    @Mock
    private ClinicalAccessService clinicalAccessService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private DeviceService deviceService;

    @Test
    void registerShouldSaveDeviceForPatient() {
        Patient patient = buildPatient(3L);
        when(userRepository.findById(3L)).thenReturn(Optional.of(patient));
        when(deviceRepository.findByMacAddress("AA:BB:CC:DD:EE:FF")).thenReturn(Optional.empty());
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
        assertEquals(Boolean.TRUE, registeredDevice.getSensorContact());
    }

    @Test
    void registerShouldRejectNonPatientReference() {
        Caregiver caregiver = buildCaregiver(4L);
        when(userRepository.findById(4L)).thenReturn(Optional.of(caregiver));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> deviceService.register(4L, "AA:BB:CC:DD:EE:FF", null)
        );

        assertEquals("Referenced user is not a patient", exception.getMessage());
        verify(deviceRepository, never()).save(any(Device.class));
    }

    @Test
    void linkDeviceShouldAuthorizeRequesterAndRecordAudit() {
        Patient patient = buildPatient(5L);
        when(clinicalAccessService.requireDeviceManagementAccess("patient5@neurolive.test", 5L)).thenReturn(patient);
        when(userRepository.findById(5L)).thenReturn(Optional.of(patient));
        when(deviceRepository.findByMacAddress("AA:BB:CC:DD:EE:05")).thenReturn(Optional.empty());
        when(deviceRepository.save(any(Device.class))).thenAnswer(invocation -> {
            Device device = invocation.getArgument(0);
            setId(device, 205L);
            return device;
        });

        Device linkedDevice = deviceService.linkDevice(
                "patient5@neurolive.test",
                5L,
                "aa:bb:cc:dd:ee:05",
                "calm-mode",
                "127.0.0.1"
        );

        assertEquals(205L, linkedDevice.getId());
        assertEquals("AA:BB:CC:DD:EE:05", linkedDevice.getMacAddress());
        verify(auditLogService).record(5L, "LINK_DEVICE", 5L, "127.0.0.1");
    }

    @Test
    void linkDeviceShouldRejectMacAlreadyLinkedToSamePatient() {
        Patient patient = buildPatient(6L);
        Device existingDevice = new Device();
        existingDevice.register(6L, "AA:BB:CC:DD:EE:06", null);

        when(clinicalAccessService.requireDeviceManagementAccess("patient6@neurolive.test", 6L)).thenReturn(patient);
        when(userRepository.findById(6L)).thenReturn(Optional.of(patient));
        when(deviceRepository.findByMacAddress("AA:BB:CC:DD:EE:06")).thenReturn(Optional.of(existingDevice));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> deviceService.linkDevice(
                        "patient6@neurolive.test",
                        6L,
                        "AA:BB:CC:DD:EE:06",
                        null,
                        "127.0.0.1"
                )
        );

        assertEquals("Device is already linked to this patient", exception.getMessage());
        verify(deviceRepository, never()).save(any(Device.class));
    }

    @Test
    void linkDeviceShouldRejectMacAlreadyLinkedToAnotherPatient() {
        Patient patient = buildPatient(7L);
        Device existingDevice = new Device();
        existingDevice.register(70L, "AA:BB:CC:DD:EE:07", null);

        when(clinicalAccessService.requireDeviceManagementAccess("patient7@neurolive.test", 7L)).thenReturn(patient);
        when(userRepository.findById(7L)).thenReturn(Optional.of(patient));
        when(deviceRepository.findByMacAddress("AA:BB:CC:DD:EE:07")).thenReturn(Optional.of(existingDevice));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> deviceService.linkDevice(
                        "patient7@neurolive.test",
                        7L,
                        "AA:BB:CC:DD:EE:07",
                        null,
                        "127.0.0.1"
                )
        );

        assertEquals("Device is already linked to another patient", exception.getMessage());
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
    void registerTelemetryShouldStoreSensorContactWhenReported() {
        Device device = new Device();
        device.register(61L, "AA:BB:CC:DD:EE:61", null);
        when(deviceRepository.findByMacAddress("AA:BB:CC:DD:EE:61")).thenReturn(Optional.of(device));
        when(deviceRepository.save(device)).thenReturn(device);
        LocalDateTime telemetryTime = LocalDateTime.of(2026, 4, 9, 18, 0);

        Device updatedDevice = deviceService.registerTelemetry("AA:BB:CC:DD:EE:61", telemetryTime, Boolean.FALSE);

        assertTrue(updatedDevice.getIsConnected());
        assertEquals(Boolean.FALSE, updatedDevice.getSensorContact());
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

    private Patient buildPatient(Long id) {
        Patient patient = new Patient();
        patient.register("Patient " + id, "patient" + id + "@neurolive.test", "encoded-secret");
        setId(patient, id);
        return patient;
    }

    private Caregiver buildCaregiver(Long id) {
        Caregiver caregiver = new Caregiver();
        caregiver.register("Caregiver " + id, "caregiver" + id + "@neurolive.test", "encoded-secret");
        setId(caregiver, id);
        return caregiver;
    }

    private void setId(Device device, Long id) {
        setField(Device.class, device, "id", id);
    }

    private void setId(User user, Long id) {
        setField(User.class, user, "id", id);
    }

    private void setField(Class<?> owner, Object target, String fieldName, Object value) {
        try {
            Field field = owner.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to set test field " + fieldName, exception);
        }
    }
}
