package com.neurolive.neuro_live_backend.business.service;

import com.neurolive.neuro_live_backend.data.enums.StatusEnum;
import com.neurolive.neuro_live_backend.data.exception.UnauthorizedAccessException;
import com.neurolive.neuro_live_backend.domain.user.Caregiver;
import com.neurolive.neuro_live_backend.domain.user.Doctor;
import com.neurolive.neuro_live_backend.domain.user.Patient;
import com.neurolive.neuro_live_backend.domain.user.PersonalUser;
import com.neurolive.neuro_live_backend.domain.user.User;
import com.neurolive.neuro_live_backend.repository.UserLinkRepository;
import com.neurolive.neuro_live_backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
// Verifica las reglas de acceso clinico: que cada rol solo acceda a los datos que le corresponden.
class ClinicalAccessServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserLinkRepository userLinkRepository;

    private ClinicalAccessService clinicalAccessService;

    @BeforeEach
    void setUp() {
        clinicalAccessService = new ClinicalAccessService(userRepository, userLinkRepository);
    }

    @Test
    void requirePatientAccess_shouldAllowPatientToAccessOwnData() {
        Patient patient = buildPatient(10L);
        when(userRepository.findByEmail("patient10@test.com")).thenReturn(Optional.of(patient));

        User result = clinicalAccessService.requirePatientAccess("patient10@test.com", 10L);

        assertEquals(patient, result);
    }

    @Test
    void requirePatientAccess_shouldRejectPatientAccessingAnotherPatient() {
        Patient patient = buildPatient(10L);
        when(userRepository.findByEmail("patient10@test.com")).thenReturn(Optional.of(patient));

        assertThrows(UnauthorizedAccessException.class,
                () -> clinicalAccessService.requirePatientAccess("patient10@test.com", 99L));
    }

    @Test
    void requirePatientAccess_shouldAllowDoctorWithActiveLink() {
        Doctor doctor = buildDoctor(20L);
        when(userRepository.findByEmail("doctor20@test.com")).thenReturn(Optional.of(doctor));
        when(userLinkRepository.existsByPatient_IdAndLinkedUser_IdAndStatus(10L, 20L, StatusEnum.ACTIVE))
                .thenReturn(true);

        User result = clinicalAccessService.requirePatientAccess("doctor20@test.com", 10L);

        assertEquals(doctor, result);
    }

    @Test
    void requirePatientAccess_shouldRejectDoctorWithoutActiveLink() {
        Doctor doctor = buildDoctor(20L);
        when(userRepository.findByEmail("doctor20@test.com")).thenReturn(Optional.of(doctor));
        when(userLinkRepository.existsByPatient_IdAndLinkedUser_IdAndStatus(10L, 20L, StatusEnum.ACTIVE))
                .thenReturn(false);

        assertThrows(UnauthorizedAccessException.class,
                () -> clinicalAccessService.requirePatientAccess("doctor20@test.com", 10L));
    }

    @Test
    void requirePatientAccess_shouldAllowCaregiverWithActiveLink() {
        Caregiver caregiver = buildCaregiver(30L);
        when(userRepository.findByEmail("caregiver30@test.com")).thenReturn(Optional.of(caregiver));
        when(userLinkRepository.existsByPatient_IdAndLinkedUser_IdAndStatus(10L, 30L, StatusEnum.ACTIVE))
                .thenReturn(true);

        User result = clinicalAccessService.requirePatientAccess("caregiver30@test.com", 10L);

        assertEquals(caregiver, result);
    }

    @Test
    void requireDeviceManagementAccess_shouldAllowPatientAccessingOwnDevices() {
        Patient patient = buildPatient(10L);
        when(userRepository.findByEmail("patient10@test.com")).thenReturn(Optional.of(patient));
        when(userRepository.findById(10L)).thenReturn(Optional.of(patient));

        User result = clinicalAccessService.requireDeviceManagementAccess("patient10@test.com", 10L);

        assertEquals(patient, result);
    }

    @Test
    void requireDeviceManagementAccess_shouldRejectPersonalUser() {
        PersonalUser personalUser = buildPersonalUser(40L);
        Patient patient = buildPatient(10L);
        when(userRepository.findByEmail("personal40@test.com")).thenReturn(Optional.of(personalUser));
        when(userRepository.findById(10L)).thenReturn(Optional.of(patient));

        assertThrows(UnauthorizedAccessException.class,
                () -> clinicalAccessService.requireDeviceManagementAccess("personal40@test.com", 10L));
    }

    @Test
    void requireThresholdManagement_shouldAllowDoctorWithActiveLink() {
        Doctor doctor = buildDoctor(20L);
        when(userRepository.findByEmail("doctor20@test.com")).thenReturn(Optional.of(doctor));
        when(userLinkRepository.existsByPatient_IdAndLinkedUser_IdAndStatus(10L, 20L, StatusEnum.ACTIVE))
                .thenReturn(true);

        User result = clinicalAccessService.requireThresholdManagement("doctor20@test.com", 10L);

        assertEquals(doctor, result);
    }

    @Test
    void requireThresholdManagement_shouldAllowPersonalUserAccessingOwnThresholds() {
        PersonalUser personalUser = buildPersonalUser(40L);
        when(userRepository.findByEmail("personal40@test.com")).thenReturn(Optional.of(personalUser));

        User result = clinicalAccessService.requireThresholdManagement("personal40@test.com", 40L);

        assertEquals(personalUser, result);
    }

    @Test
    void requireThresholdManagement_shouldRejectPersonalUserAccessingDifferentSubject() {
        PersonalUser personalUser = buildPersonalUser(40L);
        when(userRepository.findByEmail("personal40@test.com")).thenReturn(Optional.of(personalUser));

        assertThrows(UnauthorizedAccessException.class,
                () -> clinicalAccessService.requireThresholdManagement("personal40@test.com", 99L));
    }

    @Test
    void resolveCurrentUser_shouldRejectBlankEmail() {
        assertThrows(IllegalArgumentException.class,
                () -> clinicalAccessService.resolveCurrentUser("  "));
    }

    @Test
    void resolveCurrentUser_shouldReturnUserByEmail() {
        Patient patient = buildPatient(10L);
        when(userRepository.findByEmail("patient10@test.com")).thenReturn(Optional.of(patient));

        User result = clinicalAccessService.resolveCurrentUser("patient10@test.com");

        assertEquals(patient, result);
    }

    @Test
    void requirePatientAccess_shouldRejectCaregiverWithoutActiveLink() {
        Caregiver caregiver = buildCaregiver(30L);
        when(userRepository.findByEmail("caregiver30@test.com")).thenReturn(Optional.of(caregiver));
        when(userLinkRepository.existsByPatient_IdAndLinkedUser_IdAndStatus(10L, 30L, StatusEnum.ACTIVE))
                .thenReturn(false);

        assertThrows(UnauthorizedAccessException.class,
                () -> clinicalAccessService.requirePatientAccess("caregiver30@test.com", 10L));
    }

    private Patient buildPatient(Long id) {
        Patient patient = new Patient();
        patient.register("Patient " + id, "patient" + id + "@test.com", "hash");
        setId(patient, id);
        return patient;
    }

    private Doctor buildDoctor(Long id) {
        Doctor doctor = new Doctor("Neurology");
        doctor.register("Doctor " + id, "doctor" + id + "@test.com", "hash");
        setId(doctor, id);
        return doctor;
    }

    private Caregiver buildCaregiver(Long id) {
        Caregiver caregiver = new Caregiver();
        caregiver.register("Caregiver " + id, "caregiver" + id + "@test.com", "hash");
        setId(caregiver, id);
        return caregiver;
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
