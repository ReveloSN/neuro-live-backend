package com.neurolive.neuro_live_backend.business.service;

import com.neurolive.neuro_live_backend.data.enums.LinkTypeEnum;
import com.neurolive.neuro_live_backend.data.enums.StatusEnum;
import com.neurolive.neuro_live_backend.data.exception.UnauthorizedAccessException;
import com.neurolive.neuro_live_backend.domain.user.Caregiver;
import com.neurolive.neuro_live_backend.domain.user.Doctor;
import com.neurolive.neuro_live_backend.domain.user.Patient;
import com.neurolive.neuro_live_backend.domain.user.PersonalUser;
import com.neurolive.neuro_live_backend.domain.user.User;
import com.neurolive.neuro_live_backend.domain.user.UserLink;
import com.neurolive.neuro_live_backend.repository.UserLinkRepository;
import com.neurolive.neuro_live_backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
// Verifica el flujo de generacion y redencion de tokens de vinculacion.
class UserLinkServiceTest {

    @Mock
    private UserLinkRepository userLinkRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogService auditLogService;

    private UserLinkService userLinkService;

    @BeforeEach
    void setUp() {
        userLinkService = new UserLinkService(userLinkRepository, userRepository, auditLogService, 15);
    }

    @Test
    void issueTokenShouldCreatePendingLinkForAuthenticatedPatient() {
        Patient patient = buildPatient(101L);
        UserLink previousPendingLink = new UserLink(patient);
        previousPendingLink.generateToken(LocalDateTime.now().plusMinutes(5));

        when(userRepository.findByEmail("patient101@neurolive.test")).thenReturn(Optional.of(patient));
        when(userLinkRepository.findAllByPatient_IdAndStatusOrderByCreatedAtDesc(101L, StatusEnum.PENDING))
                .thenReturn(List.of(previousPendingLink));
        when(userLinkRepository.save(any(UserLink.class))).thenAnswer(invocation -> {
            UserLink userLink = invocation.getArgument(0);
            setField(userLink, UserLink.class, "id", 501L);
            return userLink;
        });

        UserLink issuedLink = userLinkService.issueToken("patient101@neurolive.test", "127.0.0.1");

        assertNotNull(issuedLink.getToken());
        assertEquals(6, issuedLink.getToken().length());
        assertTrue(issuedLink.getToken().matches("^[A-HJ-NP-Z2-9]{6}$"));
        assertEquals(StatusEnum.PENDING, issuedLink.getStatus());
        assertNotNull(issuedLink.getExpiresAt());
        assertFalse(previousPendingLink.validateToken());
        assertEquals(StatusEnum.REVOKED, previousPendingLink.getStatus());
        verify(auditLogService).record(101L, "GENERATE_LINK_TOKEN", 101L, "127.0.0.1");
    }

    @Test
    void issueTokenShouldRetryWhenGeneratedCodeCollides() {
        Patient patient = buildPatient(102L);

        when(userRepository.findByEmail("patient102@neurolive.test")).thenReturn(Optional.of(patient));
        when(userLinkRepository.findAllByPatient_IdAndStatusOrderByCreatedAtDesc(102L, StatusEnum.PENDING))
                .thenReturn(List.of());
        when(userLinkRepository.existsByToken(any(String.class))).thenReturn(true, false);
        when(userLinkRepository.save(any(UserLink.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserLink issuedLink = userLinkService.issueToken("patient102@neurolive.test", "127.0.0.1");

        assertTrue(issuedLink.getToken().matches("^[A-HJ-NP-Z2-9]{6}$"));
        verify(userLinkRepository, times(2)).existsByToken(any(String.class));
    }

    @Test
    void redeemTokenShouldActivatePendingTokenForAuthenticatedCaregiver() {
        Patient patient = buildPatient(111L);
        Caregiver caregiver = buildCaregiver(222L);
        UserLink pendingLink = new UserLink(patient);
        String token = pendingLink.generateToken(LocalDateTime.now().plusMinutes(10));

        when(userRepository.findByEmail("caregiver222@neurolive.test")).thenReturn(Optional.of(caregiver));
        when(userLinkRepository.findByToken(token)).thenReturn(Optional.of(pendingLink));
        when(userLinkRepository.existsByPatient_IdAndLinkedUser_IdAndStatus(111L, 222L, StatusEnum.ACTIVE))
                .thenReturn(false);
        when(userLinkRepository.save(any(UserLink.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserLink activatedLink = userLinkService.redeemToken("caregiver222@neurolive.test", token.toLowerCase(), "10.0.0.5");

        assertEquals(StatusEnum.ACTIVE, activatedLink.getStatus());
        assertEquals(LinkTypeEnum.CAREGIVER, activatedLink.getLinkType());
        assertEquals(222L, activatedLink.getLinkedUserId());
        assertNotNull(activatedLink.getConsumedAt());
        verify(auditLogService).record(222L, "REDEEM_LINK_TOKEN", 111L, "10.0.0.5");
    }

    @Test
    void redeemTokenShouldRejectExpiredToken() {
        Patient patient = buildPatient(121L);
        Doctor doctor = buildDoctor(333L);
        UserLink expiredLink = new UserLink(patient);
        String token = expiredLink.generateToken(LocalDateTime.now().plusMinutes(5));
        setField(expiredLink, UserLink.class, "expiresAt", LocalDateTime.now().minusMinutes(1));

        when(userRepository.findByEmail("doctor333@neurolive.test")).thenReturn(Optional.of(doctor));
        when(userLinkRepository.findByToken(token)).thenReturn(Optional.of(expiredLink));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> userLinkService.redeemToken("doctor333@neurolive.test", token, "127.0.0.1")
        );

        assertEquals("Link token has expired", exception.getMessage());
        verify(userLinkRepository, never()).save(any(UserLink.class));
    }

    @Test
    void redeemTokenShouldRejectAlreadyUsedToken() {
        Patient patient = buildPatient(122L);
        Caregiver caregiver = buildCaregiver(223L);
        UserLink usedLink = new UserLink(patient);
        String token = usedLink.generateToken(LocalDateTime.now().plusMinutes(10));
        usedLink.activate(caregiver, LocalDateTime.now());

        when(userRepository.findByEmail("caregiver223@neurolive.test")).thenReturn(Optional.of(caregiver));
        when(userLinkRepository.findByToken(token)).thenReturn(Optional.of(usedLink));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> userLinkService.redeemToken("caregiver223@neurolive.test", token, "127.0.0.1")
        );

        assertEquals("Link token has already been used", exception.getMessage());
        verify(userLinkRepository, never()).save(any(UserLink.class));
    }

    @Test
    void redeemTokenShouldRejectInvalidShortCode() {
        Doctor doctor = buildDoctor(334L);

        when(userRepository.findByEmail("doctor334@neurolive.test")).thenReturn(Optional.of(doctor));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userLinkService.redeemToken("doctor334@neurolive.test", "O0I1AA", "127.0.0.1")
        );

        assertEquals("Link token must be 6 characters using the allowed alphabet", exception.getMessage());
        verify(userLinkRepository, never()).findByToken(any(String.class));
    }

    @Test
    void redeemTokenShouldRejectPatientRequester() {
        Patient patient = buildPatient(123L);

        when(userRepository.findByEmail("patient123@neurolive.test")).thenReturn(Optional.of(patient));

        UnauthorizedAccessException exception = assertThrows(
                UnauthorizedAccessException.class,
                () -> userLinkService.redeemToken("patient123@neurolive.test", "7K4Q9P", "127.0.0.1")
        );

        assertEquals("Only caregivers and doctors can redeem link tokens", exception.getMessage());
        verify(userLinkRepository, never()).findByToken(any(String.class));
    }

    @Test
    void redeemTokenShouldRejectDuplicateActiveLink() {
        Patient patient = buildPatient(131L);
        Doctor doctor = buildDoctor(444L);
        UserLink pendingLink = new UserLink(patient);
        String token = pendingLink.generateToken(LocalDateTime.now().plusMinutes(5));

        when(userRepository.findByEmail("doctor444@neurolive.test")).thenReturn(Optional.of(doctor));
        when(userLinkRepository.findByToken(token)).thenReturn(Optional.of(pendingLink));
        when(userLinkRepository.existsByPatient_IdAndLinkedUser_IdAndStatus(131L, 444L, StatusEnum.ACTIVE))
                .thenReturn(true);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> userLinkService.redeemToken("doctor444@neurolive.test", token, "127.0.0.1")
        );

        assertEquals("A link between the current user and patient already exists", exception.getMessage());
    }

    @Test
    void redeemTokenShouldRejectPersonalUsers() {
        PersonalUser personalUser = buildPersonalUser(141L);

        when(userRepository.findByEmail("personal141@neurolive.test")).thenReturn(Optional.of(personalUser));

        UnauthorizedAccessException exception = assertThrows(
                UnauthorizedAccessException.class,
                () -> userLinkService.redeemToken("personal141@neurolive.test", "ABC12345", "127.0.0.1")
        );

        assertEquals("Only caregivers and doctors can redeem link tokens", exception.getMessage());
    }

    @Test
    void getLinksForCurrentUserShouldReturnPatientLinks() {
        Patient patient = buildPatient(151L);
        UserLink activeLink = new UserLink(patient, buildCaregiver(551L), LinkTypeEnum.CAREGIVER);
        activeLink.generateToken(LocalDateTime.now().plusMinutes(15));

        when(userRepository.findByEmail("patient151@neurolive.test")).thenReturn(Optional.of(patient));
        when(userLinkRepository.findAllByPatient_IdOrderByCreatedAtDesc(151L)).thenReturn(List.of(activeLink));

        List<UserLink> links = userLinkService.getLinksForCurrentUser("patient151@neurolive.test");

        assertEquals(1, links.size());
        assertSame(activeLink, links.getFirst());
    }

    @Test
    void revoke_shouldSetLinkToRevokedAndPersist() {
        Patient patient = buildPatient(170L);
        Caregiver caregiver = buildCaregiver(171L);
        UserLink userLink = new UserLink(patient, caregiver, LinkTypeEnum.CAREGIVER);
        userLink.generateToken(LocalDateTime.now().plusMinutes(15));
        userLink.activate();
        when(userLinkRepository.findById(500L)).thenReturn(Optional.of(userLink));
        when(userLinkRepository.save(any(UserLink.class))).thenAnswer(inv -> inv.getArgument(0));

        UserLink result = userLinkService.revoke(500L);

        assertEquals(StatusEnum.REVOKED, result.getStatus());
        verify(userLinkRepository).save(userLink);
    }

    @Test
    void revokeForRequesterShouldAllowPatientToRevokeDoctorLinkAndRecordAudit() {
        Patient patient = buildPatient(1710L);
        Doctor doctor = buildDoctor(1711L);
        UserLink userLink = buildActiveLink(patient, doctor, LinkTypeEnum.DOCTOR);

        when(userRepository.findByEmail("patient1710@neurolive.test")).thenReturn(Optional.of(patient));
        when(userLinkRepository.findById(800L)).thenReturn(Optional.of(userLink));
        when(userLinkRepository.save(userLink)).thenReturn(userLink);

        UserLink result = userLinkService.revokeForRequester("patient1710@neurolive.test", 800L, "10.1.1.1");

        assertEquals(StatusEnum.REVOKED, result.getStatus());
        assertNotNull(result.getRevokedAt());
        verify(auditLogService).record(1710L, "REVOKE_USER_LINK", 1710L, "10.1.1.1");
    }

    @Test
    void revokeForRequesterShouldAllowPatientToRevokeCaregiverLink() {
        Patient patient = buildPatient(1720L);
        Caregiver caregiver = buildCaregiver(1721L);
        UserLink userLink = buildActiveLink(patient, caregiver, LinkTypeEnum.CAREGIVER);

        when(userRepository.findByEmail("patient1720@neurolive.test")).thenReturn(Optional.of(patient));
        when(userLinkRepository.findById(801L)).thenReturn(Optional.of(userLink));
        when(userLinkRepository.save(userLink)).thenReturn(userLink);

        UserLink result = userLinkService.revokeForRequester("patient1720@neurolive.test", 801L, "10.1.1.2");

        assertEquals(StatusEnum.REVOKED, result.getStatus());
        verify(auditLogService).record(1720L, "REVOKE_USER_LINK", 1720L, "10.1.1.2");
    }

    @Test
    void revokeForRequesterShouldAllowDoctorToRevokeOwnPatientLink() {
        Patient patient = buildPatient(1730L);
        Doctor doctor = buildDoctor(1731L);
        UserLink userLink = buildActiveLink(patient, doctor, LinkTypeEnum.DOCTOR);

        when(userRepository.findByEmail("doctor1731@neurolive.test")).thenReturn(Optional.of(doctor));
        when(userLinkRepository.findById(802L)).thenReturn(Optional.of(userLink));
        when(userLinkRepository.save(userLink)).thenReturn(userLink);

        UserLink result = userLinkService.revokeForRequester("doctor1731@neurolive.test", 802L, "10.1.1.3");

        assertEquals(StatusEnum.REVOKED, result.getStatus());
        verify(auditLogService).record(1731L, "REVOKE_USER_LINK", 1730L, "10.1.1.3");
    }

    @Test
    void revokeForRequesterShouldAllowCaregiverToRevokeOwnPatientLink() {
        Patient patient = buildPatient(1740L);
        Caregiver caregiver = buildCaregiver(1741L);
        UserLink userLink = buildActiveLink(patient, caregiver, LinkTypeEnum.CAREGIVER);

        when(userRepository.findByEmail("caregiver1741@neurolive.test")).thenReturn(Optional.of(caregiver));
        when(userLinkRepository.findById(803L)).thenReturn(Optional.of(userLink));
        when(userLinkRepository.save(userLink)).thenReturn(userLink);

        UserLink result = userLinkService.revokeForRequester("caregiver1741@neurolive.test", 803L, "10.1.1.4");

        assertEquals(StatusEnum.REVOKED, result.getStatus());
        verify(auditLogService).record(1741L, "REVOKE_USER_LINK", 1740L, "10.1.1.4");
    }

    @Test
    void revokeForRequesterShouldRejectUnrelatedUser() {
        Patient patient = buildPatient(1750L);
        Doctor doctor = buildDoctor(1751L);
        Doctor unrelatedDoctor = buildDoctor(1752L);
        UserLink userLink = buildActiveLink(patient, doctor, LinkTypeEnum.DOCTOR);

        when(userRepository.findByEmail("doctor1752@neurolive.test")).thenReturn(Optional.of(unrelatedDoctor));
        when(userLinkRepository.findById(804L)).thenReturn(Optional.of(userLink));

        assertThrows(
                UnauthorizedAccessException.class,
                () -> userLinkService.revokeForRequester("doctor1752@neurolive.test", 804L, "10.1.1.5")
        );

        verify(userLinkRepository, never()).save(any(UserLink.class));
    }

    @Test
    void hasActiveLink_shouldReturnTrueWhenActiveLinkExists() {
        when(userLinkRepository.existsByPatient_IdAndLinkedUser_IdAndStatus(180L, 181L, StatusEnum.ACTIVE))
                .thenReturn(true);

        assertTrue(userLinkService.hasActiveLink(180L, 181L));
    }

    @Test
    void issueTokenShouldRejectNonPatientRequester() {
        Caregiver caregiver = buildCaregiver(161L);
        when(userRepository.findByEmail("caregiver161@neurolive.test")).thenReturn(Optional.of(caregiver));

        UnauthorizedAccessException exception = assertThrows(
                UnauthorizedAccessException.class,
                () -> userLinkService.issueToken("caregiver161@neurolive.test", "127.0.0.1")
        );

        assertEquals("Only patients can generate link tokens", exception.getMessage());
        verify(userLinkRepository, never()).save(any(UserLink.class));
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

    private PersonalUser buildPersonalUser(Long id) {
        PersonalUser personalUser = new PersonalUser();
        personalUser.register("Personal " + id, "personal" + id + "@neurolive.test", "encoded-secret");
        setId(personalUser, id);
        return personalUser;
    }

    private UserLink buildActiveLink(Patient patient, User linkedUser, LinkTypeEnum linkType) {
        UserLink userLink = new UserLink(patient, linkedUser, linkType);
        userLink.generateToken(LocalDateTime.now().plusMinutes(15));
        userLink.activate();
        return userLink;
    }

    private void setId(User user, Long id) {
        setField(user, User.class, "id", id);
    }

    private void setField(Object target, Class<?> declaringClass, String fieldName, Object value) {
        try {
            Field field = declaringClass.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to prepare test fixture", exception);
        }
    }
}
