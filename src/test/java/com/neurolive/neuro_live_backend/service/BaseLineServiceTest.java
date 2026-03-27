package com.neurolive.neuro_live_backend.service;

import com.neurolive.neuro_live_backend.entity.BaseLine;
import com.neurolive.neuro_live_backend.entity.BiometricSample;
import com.neurolive.neuro_live_backend.entity.Role;
import com.neurolive.neuro_live_backend.entity.User;
import com.neurolive.neuro_live_backend.enums.RoleName;
import com.neurolive.neuro_live_backend.repository.BaseLineRepository;
import com.neurolive.neuro_live_backend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BaseLineServiceTest {

    @Mock
    private BaseLineRepository baseLineRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BaseLineService baseLineService;

    @Test
    void shouldCalculateAndPersistBaselineWhenEnoughSamplesExist() {
        User patient = buildPatient(5L);
        when(userRepository.findById(5L)).thenReturn(Optional.of(patient));
        when(baseLineRepository.findByPatientId(5L)).thenReturn(Optional.empty());
        when(baseLineRepository.save(any(BaseLine.class))).thenAnswer(invocation -> {
            BaseLine baseLine = invocation.getArgument(0);
            setId(baseLine, 40L);
            return baseLine;
        });
        LocalDateTime sessionStart = LocalDateTime.of(2026, 3, 27, 10, 0);

        BaseLine savedBaseLine = baseLineService.calculate(5L, List.of(
                new BiometricSample(80.0f, 97.0f, sessionStart),
                new BiometricSample(82.0f, 98.0f, sessionStart.plusMinutes(1)),
                new BiometricSample(84.0f, 99.0f, sessionStart.plusMinutes(2)),
                new BiometricSample(86.0f, 98.0f, sessionStart.plusMinutes(3)),
                new BiometricSample(88.0f, 98.0f, sessionStart.plusMinutes(4)),
                new BiometricSample(90.0f, 99.0f, sessionStart.plusMinutes(5))
        ));

        assertEquals(40L, savedBaseLine.getId());
        assertTrue(savedBaseLine.isReady());
        assertEquals(85.0f, savedBaseLine.getAvgBpm(), 0.0001f);
        assertEquals(98.166664f, savedBaseLine.getAvgSpo2(), 0.0001f);
        assertEquals(sessionStart.plusMinutes(5), savedBaseLine.getCalculatedAt());
    }

    @Test
    void shouldPersistNotReadyBaselineWhenTelemetryWindowIsInsufficient() {
        User patient = buildPatient(6L);
        when(userRepository.findById(6L)).thenReturn(Optional.of(patient));
        when(baseLineRepository.findByPatientId(6L)).thenReturn(Optional.empty());
        when(baseLineRepository.save(any(BaseLine.class))).thenAnswer(invocation -> invocation.getArgument(0));
        LocalDateTime sessionStart = LocalDateTime.of(2026, 3, 27, 10, 0);

        BaseLine savedBaseLine = baseLineService.updateFromTelemetry(6L, List.of(
                new BiometricSample(79.0f, 98.0f, sessionStart),
                new BiometricSample(80.0f, 98.0f, sessionStart.plusMinutes(2)),
                new BiometricSample(81.0f, 98.0f, sessionStart.plusMinutes(4))
        ));

        assertFalse(savedBaseLine.isReady());
        assertEquals(0.0f, savedBaseLine.getAvgBpm(), 0.0001f);
        assertEquals(0.0f, savedBaseLine.getAvgSpo2(), 0.0001f);
    }

    @Test
    void shouldRejectNonPatientReference() {
        User caregiver = buildUser(8L, RoleName.CUIDADOR);
        when(userRepository.findById(8L)).thenReturn(Optional.of(caregiver));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> baseLineService.calculate(8L, List.of())
        );

        assertEquals("Referenced user is not a patient", exception.getMessage());
    }

    @Test
    void shouldReturnExistingBaselineByPatientId() {
        User patient = buildPatient(9L);
        BaseLine baseLine = new BaseLine(9L);
        when(userRepository.findById(9L)).thenReturn(Optional.of(patient));
        when(baseLineRepository.findByPatientId(9L)).thenReturn(Optional.of(baseLine));

        BaseLine storedBaseLine = baseLineService.findByPatientId(9L);

        assertSame(baseLine, storedBaseLine);
    }

    @Test
    void shouldThrowWhenBaselineDoesNotExistForPatient() {
        User patient = buildPatient(10L);
        when(userRepository.findById(10L)).thenReturn(Optional.of(patient));
        when(baseLineRepository.findByPatientId(10L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> baseLineService.findByPatientId(10L));
    }

    private User buildPatient(Long id) {
        return buildUser(id, RoleName.PACIENTE);
    }

    private User buildUser(Long id, RoleName roleName) {
        Role role = new Role();
        role.setName(roleName);

        User user = new User();
        user.setId(id);
        user.setRole(role);
        user.setActive(true);
        user.setEmail("patient" + id + "@neurolive.test");
        user.setName("Patient " + id);
        user.setPassword("secret");
        return user;
    }

    private void setId(BaseLine baseLine, Long id) {
        try {
            Field field = BaseLine.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(baseLine, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to set baseline id for test setup", exception);
        }
    }
}
