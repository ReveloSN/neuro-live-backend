package com.neurolive.neuro_live_backend.business.service;

import com.neurolive.neuro_live_backend.data.exception.UnauthorizedAccessException;
import com.neurolive.neuro_live_backend.domain.biometric.ActivationThreshold;
import com.neurolive.neuro_live_backend.domain.user.Doctor;
import com.neurolive.neuro_live_backend.domain.user.Patient;
import com.neurolive.neuro_live_backend.domain.user.PersonalUser;
import com.neurolive.neuro_live_backend.domain.user.User;
import com.neurolive.neuro_live_backend.repository.ActivationThresholdRepository;
import com.neurolive.neuro_live_backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
// Verifica la gestion de umbrales de activacion biometrica por paciente y usuario personal.
class ActivationThresholdServiceTest {

    @Mock
    private ActivationThresholdRepository activationThresholdRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ClinicalAccessService clinicalAccessService;

    private ActivationThresholdService activationThresholdService;

    @BeforeEach
    void setUp() {
        activationThresholdService = new ActivationThresholdService(
                activationThresholdRepository, userRepository, clinicalAccessService);
    }

    @Test
    void saveForPatient_shouldRejectNonDoctor() {
        PersonalUser personalUser = buildPersonalUser(40L);
        when(clinicalAccessService.requireThresholdManagement("personal@test.com", 10L))
                .thenReturn(personalUser);

        assertThrows(UnauthorizedAccessException.class,
                () -> activationThresholdService.saveForPatient(
                        "personal@test.com", 10L, 60f, 100f, 94f, 0.2f));
    }

    @Test
    void saveForPatient_shouldDeactivateExistingThresholdAndCreateNew() {
        Doctor doctor = buildDoctor(20L);
        ActivationThreshold existing = new ActivationThreshold(60f, 100f, 94f, 0.15f);
        existing.assignToPatient(10L, 20L);

        when(clinicalAccessService.requireThresholdManagement("doctor20@test.com", 10L))
                .thenReturn(doctor);
        when(activationThresholdRepository
                .findFirstByPatientIdAndActiveTrueOrderByCreatedAtDesc(10L))
                .thenReturn(Optional.of(existing));
        when(activationThresholdRepository.save(any(ActivationThreshold.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ActivationThreshold result = activationThresholdService.saveForPatient(
                "doctor20@test.com", 10L, 65f, 110f, 95f, 0.2f);

        assertFalse(existing.getActive());
        assertNotNull(result);
        verify(activationThresholdRepository, times(2)).save(any(ActivationThreshold.class));
    }

    @Test
    void saveForCurrentPersonalUser_shouldRejectNonPersonalUser() {
        Patient patient = buildPatient(1L);
        when(clinicalAccessService.resolveCurrentUser("patient1@test.com")).thenReturn(patient);

        assertThrows(UnauthorizedAccessException.class,
                () -> activationThresholdService.saveForCurrentPersonalUser(
                        "patient1@test.com", 60f, 100f, 94f, 0.2f));
    }

    @Test
    void saveForCurrentPersonalUser_shouldCreateAndAssignThresholdForPersonalUser() {
        PersonalUser personalUser = buildPersonalUser(40L);
        when(clinicalAccessService.resolveCurrentUser("personal40@test.com"))
                .thenReturn(personalUser);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        ActivationThreshold result = activationThresholdService.saveForCurrentPersonalUser(
                "personal40@test.com", 60f, 100f, 94f, 0.2f);

        assertNotNull(result);
        assertEquals(40L, result.getPersonalUserId());
        verify(userRepository).save(personalUser);
    }

    @Test
    void resolveForPatient_shouldReturnPatientSpecificThreshold() {
        ActivationThreshold patientThreshold = new ActivationThreshold(60f, 100f, 94f, 0.2f);
        patientThreshold.assignToPatient(10L, 20L);
        when(activationThresholdRepository
                .findFirstByPatientIdAndActiveTrueOrderByCreatedAtDesc(10L))
                .thenReturn(Optional.of(patientThreshold));

        ActivationThreshold result = activationThresholdService.resolveForPatient(10L);

        assertEquals(patientThreshold, result);
    }

    @Test
    void resolveForPatient_shouldFallBackToGlobalThresholdWhenNoPatientSpecificOne() {
        ActivationThreshold globalThreshold = new ActivationThreshold(55f, 120f, 92f, 0.3f);
        when(activationThresholdRepository
                .findFirstByPatientIdAndActiveTrueOrderByCreatedAtDesc(10L))
                .thenReturn(Optional.empty());
        when(activationThresholdRepository
                .findFirstByPatientIdIsNullAndPersonalUserIdIsNullAndActiveTrueOrderByCreatedAtDesc())
                .thenReturn(Optional.of(globalThreshold));

        ActivationThreshold result = activationThresholdService.resolveForPatient(10L);

        assertEquals(globalThreshold, result);
    }

    @Test
    void resolveForPatient_shouldReturnNullWhenNoThresholdExists() {
        when(activationThresholdRepository
                .findFirstByPatientIdAndActiveTrueOrderByCreatedAtDesc(10L))
                .thenReturn(Optional.empty());
        when(activationThresholdRepository
                .findFirstByPatientIdIsNullAndPersonalUserIdIsNullAndActiveTrueOrderByCreatedAtDesc())
                .thenReturn(Optional.empty());

        ActivationThreshold result = activationThresholdService.resolveForPatient(10L);

        assertNull(result);
    }

    private Doctor buildDoctor(Long id) {
        Doctor doctor = new Doctor("Neurology");
        doctor.register("Doctor " + id, "doctor" + id + "@test.com", "hash");
        setId(doctor, id);
        return doctor;
    }

    private Patient buildPatient(Long id) {
        Patient patient = new Patient();
        patient.register("Patient " + id, "patient" + id + "@test.com", "hash");
        setId(patient, id);
        return patient;
    }

    private PersonalUser buildPersonalUser(Long id) {
        PersonalUser personalUser = new PersonalUser();
        personalUser.register("Personal " + id, "personal" + id + "@test.com", "hash");
        setId(personalUser, id);
        return personalUser;
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
