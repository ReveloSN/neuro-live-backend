package com.neurolive.neuro_live_backend.business.service;

import com.neurolive.neuro_live_backend.domain.analysis.KeystrokeDynamics;
import com.neurolive.neuro_live_backend.domain.user.Patient;
import com.neurolive.neuro_live_backend.domain.user.User;
import com.neurolive.neuro_live_backend.repository.KeystrokeDynamicsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
// Verifica la captura de dinamica de tecleo con control de acceso y consentimiento.
class KeystrokeDynamicsServiceTest {

    @Mock
    private KeystrokeDynamicsRepository keystrokeDynamicsRepository;

    @Mock
    private ClinicalAccessService clinicalAccessService;

    @Mock
    private MonitoringConsentService monitoringConsentService;

    private KeystrokeDynamicsService keystrokeDynamicsService;

    @BeforeEach
    void setUp() {
        keystrokeDynamicsService = new KeystrokeDynamicsService(
                keystrokeDynamicsRepository, clinicalAccessService, monitoringConsentService);
    }

    @Test
    void capture_shouldPersistKeystrokeDynamicsForSelf() {
        Patient patient = buildPatient(1L);
        when(clinicalAccessService.resolveCurrentUser("patient1@test.com")).thenReturn(patient);
        when(keystrokeDynamicsRepository.save(any(KeystrokeDynamics.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        KeystrokeDynamics result = keystrokeDynamicsService.capture(
                "patient1@test.com", 1L, "session-A", 120f, 80f, 1, 0.05f, LocalDateTime.now());

        assertNotNull(result);
        assertEquals(1L, result.getUserId());
        verify(monitoringConsentService).assertMonitoringAllowed(1L);
        verify(keystrokeDynamicsRepository).save(any(KeystrokeDynamics.class));
    }

    @Test
    void capture_shouldPropagateConsentExceptionBeforeSaving() {
        Patient patient = buildPatient(1L);
        when(clinicalAccessService.resolveCurrentUser("patient1@test.com")).thenReturn(patient);
        doThrow(new IllegalStateException("Biometric consent is required before monitoring starts"))
                .when(monitoringConsentService).assertMonitoringAllowed(1L);

        assertThrows(IllegalStateException.class,
                () -> keystrokeDynamicsService.capture(
                        "patient1@test.com", 1L, null, 120f, 80f, 0, 0.0f, LocalDateTime.now()));
    }

    @Test
    void findRecentForUser_shouldRejectNonPositiveLimit() {
        assertThrows(IllegalArgumentException.class,
                () -> keystrokeDynamicsService.findRecentForUser(1L, 0));
    }

    private Patient buildPatient(Long id) {
        Patient patient = new Patient();
        patient.register("Patient " + id, "patient" + id + "@test.com", "hash");
        try {
            Field field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(patient, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to prepare test fixture", e);
        }
        return patient;
    }

    @Test
    void findRecentForUser_shouldReturnAtMostTheRequestedNumberOfEntries() {
        KeystrokeDynamics ks1 = KeystrokeDynamics.capture(1L, 120f, 80f, LocalDateTime.now());
        KeystrokeDynamics ks2 = KeystrokeDynamics.capture(1L, 130f, 90f, LocalDateTime.now().minusSeconds(5));
        KeystrokeDynamics ks3 = KeystrokeDynamics.capture(1L, 140f, 100f, LocalDateTime.now().minusSeconds(10));
        when(keystrokeDynamicsRepository.findAllByUserIdOrderByTimestampDesc(1L))
                .thenReturn(List.of(ks1, ks2, ks3));

        List<KeystrokeDynamics> result = keystrokeDynamicsService.findRecentForUser(1L, 2);

        assertEquals(2, result.size());
    }
}
