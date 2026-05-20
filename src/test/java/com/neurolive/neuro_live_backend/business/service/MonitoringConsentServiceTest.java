package com.neurolive.neuro_live_backend.business.service;

import com.neurolive.neuro_live_backend.domain.user.Caregiver;
import com.neurolive.neuro_live_backend.domain.user.Patient;
import com.neurolive.neuro_live_backend.domain.user.User;
import com.neurolive.neuro_live_backend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
// Verifica la asercion y el registro del consentimiento de monitoreo biometrico.
class MonitoringConsentServiceTest {

    @Mock
    private UserRepository userRepository;

    private MonitoringConsentService monitoringConsentService;

    @BeforeEach
    void setUp() {
        monitoringConsentService = new MonitoringConsentService(userRepository);
    }

    @Test
    void assertMonitoringAllowed_shouldPassForPatientWithConsent() {
        Patient patient = buildPatient(1L);
        patient.giveConsent();
        when(userRepository.findById(1L)).thenReturn(Optional.of(patient));

        assertDoesNotThrow(() -> monitoringConsentService.assertMonitoringAllowed(1L));
    }

    @Test
    void assertMonitoringAllowed_shouldThrowForPatientWithoutConsent() {
        Patient patient = buildPatient(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(patient));

        assertThrows(IllegalStateException.class,
                () -> monitoringConsentService.assertMonitoringAllowed(1L));
    }

    @Test
    void assertMonitoringAllowed_shouldPassForNonPatientUser() {
        Caregiver caregiver = new Caregiver();
        caregiver.register("Caregiver", "care@test.com", "hash");
        setId(caregiver, 2L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(caregiver));

        assertDoesNotThrow(() -> monitoringConsentService.assertMonitoringAllowed(2L));
    }

    @Test
    void registerPatientConsent_shouldGrantConsentAndReturnSavedPatient() {
        Patient patient = buildPatient(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        Patient result = monitoringConsentService.registerPatientConsent(1L);

        assertTrue(result.getConsentGiven());
        verify(userRepository).save(patient);
    }

    @Test
    void assertMonitoringAllowed_shouldThrowWhenUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> monitoringConsentService.assertMonitoringAllowed(99L));
    }

    @Test
    void registerPatientConsent_shouldPreserveConsentDateOnRepeat() {
        Patient patient = buildPatient(1L);
        patient.giveConsent();
        when(userRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        Patient result = monitoringConsentService.registerPatientConsent(1L);

        assertTrue(result.getConsentGiven());
        assertNotNull(result.getConsentDate());
    }

    @Test
    void registerPatientConsent_shouldRejectNonPatient() {
        Caregiver caregiver = new Caregiver();
        caregiver.register("Caregiver", "care@test.com", "hash");
        setId(caregiver, 2L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(caregiver));

        assertThrows(IllegalArgumentException.class,
                () -> monitoringConsentService.registerPatientConsent(2L));
    }

    private Patient buildPatient(Long id) {
        Patient patient = new Patient();
        patient.register("Patient " + id, "patient" + id + "@test.com", "hash");
        setId(patient, id);
        return patient;
    }

    private void setId(User user, Long id) {
        try {
            Field field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to prepare test fixture", e);
        }
    }
}
