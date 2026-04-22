package com.neurolive.neuro_live_backend.business.service;

import com.neurolive.neuro_live_backend.domain.user.AccountRecoveryToken;
import com.neurolive.neuro_live_backend.domain.user.Patient;
import com.neurolive.neuro_live_backend.domain.user.User;
import com.neurolive.neuro_live_backend.repository.AccountRecoveryTokenRepository;
import com.neurolive.neuro_live_backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
// Cubre los escenarios principales del flujo de recuperacion de cuenta.
class AccountRecoveryServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountRecoveryTokenRepository accountRecoveryTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AccountRecoveryMailService accountRecoveryMailService;

    private AccountRecoveryService accountRecoveryService;

    @BeforeEach
    void setUp() {
        accountRecoveryService = new AccountRecoveryService(
                userRepository,
                accountRecoveryTokenRepository,
                passwordEncoder,
                accountRecoveryMailService,
                15,
                60
        );
    }

    @Test
    void requestRecoveryShouldSendMailAndReturnGenericMessageForRegisteredEmail() {
        Patient user = buildPatient(301L, "patient301@neurolive.test");

        when(userRepository.findByEmail("patient301@neurolive.test")).thenReturn(Optional.of(user));
        when(accountRecoveryTokenRepository.findFirstByEmailAndConsumedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
                eq("patient301@neurolive.test"),
                any(LocalDateTime.class)
        )).thenReturn(Optional.empty());
        when(accountRecoveryTokenRepository.findAllByUserIdAndConsumedAtIsNullAndExpiresAtAfter(
                eq(301L),
                any(LocalDateTime.class)
        )).thenReturn(List.of());
        when(accountRecoveryTokenRepository.save(any(AccountRecoveryToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AccountRecoveryService.RecoveryStatus status = accountRecoveryService.requestRecovery("patient301@neurolive.test");

        ArgumentCaptor<AccountRecoveryToken> tokenCaptor = ArgumentCaptor.forClass(AccountRecoveryToken.class);
        ArgumentCaptor<String> rawTokenCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<LocalDateTime> expirationCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

        verify(accountRecoveryTokenRepository).save(tokenCaptor.capture());
        verify(accountRecoveryMailService).sendRecoveryToken(
                org.mockito.ArgumentMatchers.eq("patient301@neurolive.test"),
                rawTokenCaptor.capture(),
                expirationCaptor.capture()
        );

        assertEquals("If the email exists, recovery instructions were sent", status.message());
        assertTrue(status.valid());
        assertNull(status.expiresAt());
        assertNotNull(rawTokenCaptor.getValue());
        assertNotEquals(rawTokenCaptor.getValue(), tokenCaptor.getValue().getTokenHash());
        assertEquals(expirationCaptor.getValue(), tokenCaptor.getValue().getExpiresAt());
    }

    @Test
    void requestRecoveryShouldNotRevealUnknownEmail() {
        when(userRepository.findByEmail("missing@neurolive.test")).thenReturn(Optional.empty());

        AccountRecoveryService.RecoveryStatus status = accountRecoveryService.requestRecovery("missing@neurolive.test");

        assertEquals("If the email exists, recovery instructions were sent", status.message());
        assertTrue(status.valid());
        assertNull(status.expiresAt());
        verify(accountRecoveryTokenRepository, never()).save(any(AccountRecoveryToken.class));
        verify(accountRecoveryMailService, never()).sendRecoveryToken(anyString(), anyString(), any(LocalDateTime.class));
    }

    @Test
    void requestRecoveryShouldThrottleRecentRecoveryRequests() {
        Patient user = buildPatient(302L, "patient302@neurolive.test");
        AccountRecoveryToken activeToken = AccountRecoveryToken.issue(
                302L,
                "patient302@neurolive.test",
                hash("existing-token"),
                LocalDateTime.now().plusMinutes(10)
        );
        setField(activeToken, AccountRecoveryToken.class, "createdAt", LocalDateTime.now().minusSeconds(30));

        when(userRepository.findByEmail("patient302@neurolive.test")).thenReturn(Optional.of(user));
        when(accountRecoveryTokenRepository.findFirstByEmailAndConsumedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
                eq("patient302@neurolive.test"),
                any(LocalDateTime.class)
        )).thenReturn(Optional.of(activeToken));

        AccountRecoveryService.RecoveryStatus status = accountRecoveryService.requestRecovery("patient302@neurolive.test");

        assertEquals("If the email exists, recovery instructions were sent", status.message());
        assertTrue(status.valid());
        verify(accountRecoveryTokenRepository, never()).save(any(AccountRecoveryToken.class));
        verify(accountRecoveryMailService, never()).sendRecoveryToken(anyString(), anyString(), any(LocalDateTime.class));
    }

    @Test
    void validateTokenShouldAcceptActiveToken() {
        String rawToken = "valid-recovery-token";
        AccountRecoveryToken recoveryToken = AccountRecoveryToken.issue(
                303L,
                "patient303@neurolive.test",
                hash(rawToken),
                LocalDateTime.now().plusMinutes(5)
        );

        when(accountRecoveryTokenRepository.findFirstByEmailAndConsumedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
                eq("patient303@neurolive.test"),
                any(LocalDateTime.class)
        )).thenReturn(Optional.of(recoveryToken));

        AccountRecoveryService.RecoveryStatus status =
                accountRecoveryService.validateToken("patient303@neurolive.test", rawToken);

        assertEquals("Recovery token is valid", status.message());
        assertTrue(status.valid());
        assertEquals(recoveryToken.getExpiresAt(), status.expiresAt());
    }

    @Test
    void validateTokenShouldRejectInvalidToken() {
        AccountRecoveryToken recoveryToken = AccountRecoveryToken.issue(
                304L,
                "patient304@neurolive.test",
                hash("correct-token"),
                LocalDateTime.now().plusMinutes(5)
        );

        when(accountRecoveryTokenRepository.findFirstByEmailAndConsumedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
                eq("patient304@neurolive.test"),
                any(LocalDateTime.class)
        )).thenReturn(Optional.of(recoveryToken));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> accountRecoveryService.validateToken("patient304@neurolive.test", "wrong-token")
        );

        assertEquals("Recovery token is invalid or expired", exception.getMessage());
    }

    @Test
    void resetPasswordShouldEncodePasswordAndConsumeToken() {
        String rawToken = "usable-token";
        Patient user = buildPatient(305L, "patient305@neurolive.test");
        AccountRecoveryToken recoveryToken = AccountRecoveryToken.issue(
                305L,
                "patient305@neurolive.test",
                hash(rawToken),
                LocalDateTime.now().plusMinutes(5)
        );

        when(accountRecoveryTokenRepository.findFirstByEmailAndConsumedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
                eq("patient305@neurolive.test"),
                any(LocalDateTime.class)
        )).thenReturn(Optional.of(recoveryToken));
        when(userRepository.findByEmail("patient305@neurolive.test")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("new-secret")).thenReturn("encoded-secret");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(accountRecoveryTokenRepository.save(any(AccountRecoveryToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        accountRecoveryService.resetPassword("patient305@neurolive.test", rawToken, "new-secret");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        ArgumentCaptor<AccountRecoveryToken> tokenCaptor = ArgumentCaptor.forClass(AccountRecoveryToken.class);
        verify(userRepository).save(userCaptor.capture());
        verify(accountRecoveryTokenRepository).save(tokenCaptor.capture());

        assertEquals("encoded-secret", userCaptor.getValue().getPasswordHash());
        assertNotNull(tokenCaptor.getValue().getConsumedAt());
    }

    @Test
    void resetPasswordShouldRejectConsumedToken() {
        String rawToken = "used-token";
        AccountRecoveryToken consumedToken = AccountRecoveryToken.issue(
                306L,
                "patient306@neurolive.test",
                hash(rawToken),
                LocalDateTime.now().plusMinutes(5)
        );
        consumedToken.consume(LocalDateTime.now());

        when(accountRecoveryTokenRepository.findFirstByEmailAndConsumedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
                eq("patient306@neurolive.test"),
                any(LocalDateTime.class)
        )).thenReturn(Optional.of(consumedToken));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> accountRecoveryService.resetPassword("patient306@neurolive.test", rawToken, "new-secret")
        );

        assertEquals("Recovery token is invalid or expired", exception.getMessage());
    }

    @Test
    void resetPasswordShouldRejectExpiredToken() {
        when(accountRecoveryTokenRepository.findFirstByEmailAndConsumedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
                eq("patient307@neurolive.test"),
                any(LocalDateTime.class)
        )).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> accountRecoveryService.resetPassword("patient307@neurolive.test", "expired-token", "new-secret")
        );

        assertEquals("Recovery token is invalid or expired", exception.getMessage());
    }

    private Patient buildPatient(Long id, String email) {
        Patient patient = new Patient();
        patient.register("Patient " + id, email, "encoded-current");
        setField(patient, User.class, "id", id);
        return patient;
    }

    private String hash(String rawToken) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] digest = messageDigest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Unable to hash token in test", exception);
        }
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
