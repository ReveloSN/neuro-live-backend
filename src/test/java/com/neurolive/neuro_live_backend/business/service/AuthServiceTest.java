package com.neurolive.neuro_live_backend.business.service;

import com.neurolive.neuro_live_backend.data.exception.AuthenticationFailedException;
import com.neurolive.neuro_live_backend.data.enums.RoleEnum;
import com.neurolive.neuro_live_backend.domain.user.Patient;
import com.neurolive.neuro_live_backend.domain.user.PersonalUser;
import com.neurolive.neuro_live_backend.domain.user.User;
import com.neurolive.neuro_live_backend.infrastructure.security.JwtService;
import com.neurolive.neuro_live_backend.presentation.dto.LoginRequest;
import com.neurolive.neuro_live_backend.presentation.dto.LoginResponse;
import com.neurolive.neuro_live_backend.presentation.dto.RegisterRequest;
import com.neurolive.neuro_live_backend.presentation.dto.RegisterResponse;
import com.neurolive.neuro_live_backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
// Prueba los casos principales del servicio de autenticacion.
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerShouldPersistPatientSubclassForPatientRole() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Patient One");
        request.setEmail("patient@neurolive.test");
        request.setPassword("plain-secret");
        request.setRole(RoleEnum.PATIENT);

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode("plain-secret")).thenReturn("encoded-secret");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            setId(user, 11L);
            return user;
        });

        RegisterResponse response = authService.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertInstanceOf(Patient.class, userCaptor.getValue());
        assertEquals("encoded-secret", userCaptor.getValue().getPasswordHash());
        assertEquals(RoleEnum.PATIENT, userCaptor.getValue().getRole());
        assertEquals(11L, response.getId());
        assertEquals("PATIENT", response.getRole());
    }

    @Test
    void registerShouldPersistPersonalUserSubclassForPersonalRole() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Personal User");
        request.setEmail("personal@neurolive.test");
        request.setPassword("plain-secret");
        request.setRole(RoleEnum.USER_PERSONAL);

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode("plain-secret")).thenReturn("encoded-secret");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authService.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertInstanceOf(PersonalUser.class, userCaptor.getValue());
        assertEquals(RoleEnum.USER_PERSONAL, userCaptor.getValue().getRole());
    }

    @Test
    void registerShouldRaiseConflictWhenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest();
        request.setName("Patient One");
        request.setEmail("patient@neurolive.test");
        request.setPassword("plain-secret");
        request.setRole(RoleEnum.PATIENT);

        when(userRepository.existsByEmail("patient@neurolive.test")).thenReturn(true);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> authService.register(request)
        );

        assertEquals("Email already registered", exception.getMessage());
    }

    @Test
    void loginShouldReturnTokenWhenCredentialsAreValid() {
        LoginRequest request = new LoginRequest();
        request.setEmail("patient@neurolive.test");
        request.setPassword("plain-secret");

        Patient patient = new Patient();
        patient.register("Patient One", request.getEmail(), "$2a$10$encoded-secret");
        setId(patient, 13L);

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(patient));
        when(passwordEncoder.matches("plain-secret", "$2a$10$encoded-secret")).thenReturn(true);
        when(jwtService.generateToken(patient)).thenReturn("jwt-token");

        LoginResponse response = authService.login(request);

        assertEquals(13L, response.getId());
        assertEquals("jwt-token", response.getToken());
        assertEquals("PATIENT", response.getRole());
    }

    @Test
    void loginShouldNormalizeEmailBeforeLookup() {
        LoginRequest request = new LoginRequest();
        request.setEmail("Patient@NeuroLive.Test");
        request.setPassword("plain-secret");

        Patient patient = new Patient();
        patient.register("Patient One", "patient@neurolive.test", "$2a$10$encoded-secret");
        setId(patient, 13L);

        when(userRepository.findByEmail(eq("patient@neurolive.test"))).thenReturn(Optional.of(patient));
        when(passwordEncoder.matches("plain-secret", "$2a$10$encoded-secret")).thenReturn(true);
        when(jwtService.generateToken(patient)).thenReturn("jwt-token");

        LoginResponse response = authService.login(request);

        assertEquals(13L, response.getId());
        assertEquals("patient@neurolive.test", response.getEmail());
        verify(userRepository).findByEmail("patient@neurolive.test");
    }

    @Test
    void loginShouldRejectInvalidCredentials() {
        LoginRequest request = new LoginRequest();
        request.setEmail("patient@neurolive.test");
        request.setPassword("wrong-secret");

        Patient patient = new Patient();
        patient.register("Patient One", request.getEmail(), "$2a$10$encoded-secret");
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(patient));
        when(passwordEncoder.matches("wrong-secret", "$2a$10$encoded-secret")).thenReturn(false);

        assertThrows(AuthenticationFailedException.class, () -> authService.login(request));
    }

    @Test
    void loginShouldUpgradeLegacyPlaintextPasswordHash() {
        LoginRequest request = new LoginRequest();
        request.setEmail("patient@neurolive.test");
        request.setPassword("plain-secret");

        Patient patient = new Patient();
        patient.register("Patient One", request.getEmail(), "plain-secret");
        setId(patient, 15L);

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(patient));
        when(passwordEncoder.encode("plain-secret")).thenReturn("encoded-secret");
        when(userRepository.save(patient)).thenReturn(patient);
        when(jwtService.generateToken(patient)).thenReturn("jwt-token");

        LoginResponse response = authService.login(request);

        assertEquals(15L, response.getId());
        assertEquals("jwt-token", response.getToken());
        assertEquals("encoded-secret", patient.getPasswordHash());
        verify(userRepository).save(patient);
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
