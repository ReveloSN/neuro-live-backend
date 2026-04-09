package com.neurolive.neuro_live_backend.domain.user;

import com.neurolive.neuro_live_backend.data.enums.LinkTypeEnum;
import com.neurolive.neuro_live_backend.data.enums.StatusEnum;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Valida las reglas de negocio del vinculo entre paciente y usuario.
class UserLinkTest {

    @Test
    void generateTokenShouldCreatePendingTokenUsingPatientLogic() {
        Patient patient = buildPatient(21L);
        Caregiver caregiver = buildCaregiver(22L);
        UserLink userLink = new UserLink(patient, caregiver, LinkTypeEnum.CAREGIVER);

        String token = userLink.generateToken();

        assertNotNull(token);
        assertEquals(20, token.length());
        assertEquals(StatusEnum.PENDING, userLink.getStatus());
        assertNotNull(userLink.getExpiresAt());
        assertTrue(userLink.validateToken(token));
    }

    @Test
    void activateShouldMovePendingLinkToActive() {
        UserLink userLink = new UserLink(buildPatient(23L), buildCaregiver(24L), LinkTypeEnum.CAREGIVER);
        userLink.generateToken();

        userLink.activate();

        assertEquals(StatusEnum.ACTIVE, userLink.getStatus());
        assertNotNull(userLink.getConsumedAt());
        assertFalse(userLink.validateToken());
    }

    @Test
    void activateShouldAttachLinkedUserToGenericPatientToken() {
        Patient patient = buildPatient(27L);
        Caregiver caregiver = buildCaregiver(28L);
        UserLink userLink = new UserLink(patient);
        userLink.generateToken(LocalDateTime.now().plusMinutes(10));

        userLink.activate(caregiver, LocalDateTime.now());

        assertEquals(StatusEnum.ACTIVE, userLink.getStatus());
        assertEquals(LinkTypeEnum.CAREGIVER, userLink.getLinkType());
        assertEquals(28L, userLink.getLinkedUserId());
    }

    @Test
    void validateTokenShouldRejectExpiredToken() {
        Patient patient = buildPatient(29L);
        UserLink userLink = new UserLink(patient);
        String token = userLink.generateToken(LocalDateTime.now().plusMinutes(1));

        assertFalse(userLink.validateToken(token, LocalDateTime.now().plusMinutes(2)));
    }

    @Test
    void constructorShouldRejectMismatchedLinkedRole() {
        Patient patient = buildPatient(25L);
        Doctor doctor = buildDoctor(26L);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new UserLink(patient, doctor, LinkTypeEnum.CAREGIVER)
        );

        assertEquals("Linked user role does not match the link type", exception.getMessage());
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

    private Doctor buildDoctor(Long id) {
        Doctor doctor = new Doctor("Neurology");
        doctor.register("Doctor " + id, "doctor" + id + "@neurolive.test", "encoded-secret");
        setId(doctor, id);
        return doctor;
    }

    private void setId(User user, Long id) {
        try {
            Field field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to set user id for test setup", exception);
        }
    }
}
