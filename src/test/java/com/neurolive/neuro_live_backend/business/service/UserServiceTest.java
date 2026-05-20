package com.neurolive.neuro_live_backend.business.service;

import com.neurolive.neuro_live_backend.domain.user.Doctor;
import com.neurolive.neuro_live_backend.domain.user.Patient;
import com.neurolive.neuro_live_backend.domain.user.User;
import com.neurolive.neuro_live_backend.presentation.dto.UpdateUserProfileRequest;
import com.neurolive.neuro_live_backend.presentation.dto.UserProfileResponse;
import com.neurolive.neuro_live_backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
// Verifica la gestion de perfiles de usuario, especialidades de médico y consentimiento de paciente.
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder);
    }

    @Test
    void getCurrentUserProfile_shouldReturnProfileForAuthenticatedUser() {
        Patient patient = buildPatient(1L);
        when(userRepository.findByEmail("patient1@test.com")).thenReturn(Optional.of(patient));

        UserProfileResponse profile = userService.getCurrentUserProfile("patient1@test.com");

        assertNotNull(profile);
        assertEquals(1L, profile.getId());
        assertEquals("Patient 1", profile.getName());
    }

    @Test
    void updateCurrentUserProfile_shouldPersistUpdatedNameAndPhotoUrl() {
        Patient patient = buildPatient(1L);
        when(userRepository.findByEmail("patient1@test.com")).thenReturn(Optional.of(patient));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateUserProfileRequest request = new UpdateUserProfileRequest();
        request.setName("Updated Name");
        request.setPhotoUrl("https://cdn.test/photo.png");

        UserProfileResponse result = userService.updateCurrentUserProfile("patient1@test.com", request);

        assertEquals("Updated Name", result.getName());
        verify(userRepository).save(patient);
    }

    @Test
    void recoverAccount_shouldRejectBlankPassword() {
        assertThrows(IllegalArgumentException.class,
                () -> userService.recoverAccount("patient1@test.com", "  "));
    }

    @Test
    void recoverAccount_shouldEncodePasswordAndSave() {
        Patient patient = buildPatient(1L);
        when(userRepository.findByEmail("patient1@test.com")).thenReturn(Optional.of(patient));
        when(passwordEncoder.encode("newPass")).thenReturn("encoded-newPass");

        userService.recoverAccount("patient1@test.com", "newPass");

        verify(passwordEncoder).encode("newPass");
        verify(userRepository).save(patient);
    }

    @Test
    void getDoctorSpecialty_shouldRejectNonDoctor() {
        Patient patient = buildPatient(1L);
        when(userRepository.findByEmail("patient1@test.com")).thenReturn(Optional.of(patient));

        assertThrows(IllegalArgumentException.class,
                () -> userService.getDoctorSpecialty("patient1@test.com"));
    }

    @Test
    void getDoctorSpecialty_shouldReturnSpecialtyForDoctor() {
        Doctor doctor = buildDoctor(2L, "Cardiology");
        when(userRepository.findByEmail("doctor2@test.com")).thenReturn(Optional.of(doctor));

        String specialty = userService.getDoctorSpecialty("doctor2@test.com");

        assertEquals("Cardiology", specialty);
    }

    @Test
    void updateDoctorSpecialty_shouldUpdateAndReturn() {
        Doctor doctor = buildDoctor(2L, "Neurology");
        when(userRepository.findByEmail("doctor2@test.com")).thenReturn(Optional.of(doctor));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        String updated = userService.updateDoctorSpecialty("doctor2@test.com", "Pediatrics");

        assertEquals("Pediatrics", updated);
        verify(userRepository).save(doctor);
    }

    @Test
    void grantPatientConsent_shouldSetConsentAndPersist() {
        Patient patient = buildPatient(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserService.PatientConsentResponse response = userService.grantPatientConsent("anyone@test.com", 1L);

        assertTrue(response.consentGiven());
        assertNotNull(response.consentDate());
        verify(userRepository).save(patient);
    }

    @Test
    void getPatientConsent_shouldRejectNonPatient() {
        Doctor doctor = buildDoctor(2L, "Neurology");
        when(userRepository.findById(2L)).thenReturn(Optional.of(doctor));

        assertThrows(IllegalArgumentException.class,
                () -> userService.getPatientConsent("anyone@test.com", 2L));
    }

    @Test
    void getPatientConsent_shouldReturnConsentStatusForPatient() {
        Patient patient = buildPatient(1L);
        patient.giveConsent();
        when(userRepository.findById(1L)).thenReturn(Optional.of(patient));

        UserService.PatientConsentResponse response = userService.getPatientConsent("anyone@test.com", 1L);

        assertTrue(response.consentGiven());
        assertNotNull(response.consentDate());
        assertEquals(1L, response.patientId());
    }

    @Test
    void updateCurrentUserProfile_shouldRejectBlankName() {
        Patient patient = buildPatient(1L);
        when(userRepository.findByEmail("patient1@test.com")).thenReturn(Optional.of(patient));

        UpdateUserProfileRequest request = new UpdateUserProfileRequest();
        request.setName("  ");
        request.setPhotoUrl(null);

        assertThrows(IllegalArgumentException.class,
                () -> userService.updateCurrentUserProfile("patient1@test.com", request));
    }

    private Patient buildPatient(Long id) {
        Patient patient = new Patient();
        patient.register("Patient " + id, "patient" + id + "@test.com", "hash");
        setId(patient, id);
        return patient;
    }

    private Doctor buildDoctor(Long id, String specialty) {
        Doctor doctor = new Doctor(specialty);
        doctor.register("Doctor " + id, "doctor" + id + "@test.com", "hash");
        setId(doctor, id);
        return doctor;
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
