package com.neurolive.neuro_live_backend.business.service;

import com.neurolive.neuro_live_backend.data.enums.LinkTypeEnum;
import com.neurolive.neuro_live_backend.data.enums.RoleEnum;
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
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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
        assertEquals(StatusEnum.PENDING, issuedLink.getStatus());
        assertNotNull(issuedLink.getExpiresAt());
        assertFalse(previousPendingLink.validateToken());
        assertEquals(StatusEnum.REVOKED, previousPendingLink.getStatus());
        verify(auditLogService).record(101L, "GENERATE_LINK_TOKEN", 101L, "127.0.0.1");
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
