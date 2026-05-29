package com.neurolive.neuro_live_backend.business.service;

import com.neurolive.neuro_live_backend.business.analysis.BaselineCalculator;
import com.neurolive.neuro_live_backend.domain.biometric.BaseLine;
import com.neurolive.neuro_live_backend.domain.biometric.BiometricData;
import com.neurolive.neuro_live_backend.domain.user.Caregiver;
import com.neurolive.neuro_live_backend.domain.user.Patient;
import com.neurolive.neuro_live_backend.domain.user.User;
import com.neurolive.neuro_live_backend.repository.BaseLineRepository;
import com.neurolive.neuro_live_backend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.persistence.EntityNotFoundException;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
// Verifica la logica del servicio que calcula lineas base.
class BaseLineServiceTest {

    @Mock
    private BaseLineRepository baseLineRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BaselineCalculator baselineCalculator;

    @InjectMocks
    private BaseLineService baseLineService;

    @Test
    void shouldCalculateAndPersistBaselineWhenEnoughSamplesExist() {
        Patient patient = buildPatient(5L);
        when(userRepository.findById(5L)).thenReturn(Optional.of(patient));
        when(baseLineRepository.findByPatientId(5L)).thenReturn(Optional.empty());
        when(baselineCalculator.calculate(any(BaseLine.class), anyCollection())).thenAnswer(invocation -> {
            BaseLine baseLine = invocation.getArgument(0);
            @SuppressWarnings("unchecked")
            List<BiometricData> samples = (List<BiometricData>) invocation.getArgument(1);
            baseLine.calculate(samples);
            return baseLine;
        });
        when(baseLineRepository.save(any(BaseLine.class))).thenAnswer(invocation -> {
            BaseLine baseLine = invocation.getArgument(0);
            setId(baseLine, 40L);
            return baseLine;
        });
        Instant sessionStart = Instant.parse("2026-03-27T10:00:00Z");

        BaseLine savedBaseLine = baseLineService.calculate(5L, List.of(
                new BiometricData(80.0f, 97.0f, sessionStart),
                new BiometricData(82.0f, 98.0f, sessionStart.plusSeconds(60)),
                new BiometricData(84.0f, 99.0f, sessionStart.plusSeconds(120)),
                new BiometricData(86.0f, 98.0f, sessionStart.plusSeconds(180)),
                new BiometricData(88.0f, 98.0f, sessionStart.plusSeconds(240)),
                new BiometricData(90.0f, 99.0f, sessionStart.plusSeconds(300))
        ));

        assertEquals(40L, savedBaseLine.getId());
        assertTrue(savedBaseLine.isReady());
        assertEquals(85.0f, savedBaseLine.getAvgBpm(), 0.0001f);
        assertEquals(98.166664f, savedBaseLine.getAvgSpo2(), 0.0001f);
        assertEquals(sessionStart.plusSeconds(300), savedBaseLine.getCalculatedAt());
    }

    @Test
    void shouldPersistNotReadyBaselineWhenTelemetryWindowIsInsufficient() {
        Patient patient = buildPatient(6L);
        when(userRepository.findById(6L)).thenReturn(Optional.of(patient));
        when(baseLineRepository.findByPatientId(6L)).thenReturn(Optional.empty());
        when(baselineCalculator.calculate(any(BaseLine.class), anyCollection())).thenAnswer(invocation -> {
            BaseLine baseLine = invocation.getArgument(0);
            @SuppressWarnings("unchecked")
            List<BiometricData> samples = (List<BiometricData>) invocation.getArgument(1);
            baseLine.calculate(samples);
            return baseLine;
        });
        when(baseLineRepository.save(any(BaseLine.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Instant sessionStart = Instant.parse("2026-03-27T10:00:00Z");

        BaseLine savedBaseLine = baseLineService.updateFromTelemetry(6L, List.of(
                new BiometricData(79.0f, 98.0f, sessionStart),
                new BiometricData(80.0f, 98.0f, sessionStart.plusSeconds(120)),
                new BiometricData(81.0f, 98.0f, sessionStart.plusSeconds(240))
        ));

        assertFalse(savedBaseLine.isReady());
        assertEquals(0.0f, savedBaseLine.getAvgBpm(), 0.0001f);
        assertEquals(0.0f, savedBaseLine.getAvgSpo2(), 0.0001f);
    }

    @Test
    void shouldRejectNonPatientReference() {
        Caregiver caregiver = buildCaregiver(8L);
        when(userRepository.findById(8L)).thenReturn(Optional.of(caregiver));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> baseLineService.calculate(8L, List.of())
        );

        assertEquals("Referenced user is not a patient", exception.getMessage());
    }

    @Test
    void shouldReturnExistingBaselineByPatientId() {
        Patient patient = buildPatient(9L);
        BaseLine baseLine = new BaseLine(9L);
        when(userRepository.findById(9L)).thenReturn(Optional.of(patient));
        when(baseLineRepository.findByPatientId(9L)).thenReturn(Optional.of(baseLine));

        BaseLine storedBaseLine = baseLineService.findByPatientId(9L);

        assertSame(baseLine, storedBaseLine);
    }

    @Test
    void shouldThrowWhenBaselineDoesNotExistForPatient() {
        Patient patient = buildPatient(10L);
        when(userRepository.findById(10L)).thenReturn(Optional.of(patient));
        when(baseLineRepository.findByPatientId(10L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> baseLineService.findByPatientId(10L));
    }

    @Test
    void shouldThrowWhenPatientNotFoundForCalculation() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> baseLineService.calculate(99L, List.of()));
    }

    @Test
    void shouldReuseExistingBaselineWhenPresent() {
        Patient patient = buildPatient(11L);
        BaseLine existing = new BaseLine(11L);
        when(userRepository.findById(11L)).thenReturn(Optional.of(patient));
        when(baseLineRepository.findByPatientId(11L)).thenReturn(Optional.of(existing));
        when(baselineCalculator.calculate(any(BaseLine.class), anyCollection()))
                .thenAnswer(inv -> inv.getArgument(0));
        when(baseLineRepository.save(any(BaseLine.class))).thenAnswer(inv -> inv.getArgument(0));

        BaseLine result = baseLineService.calculate(11L, List.of());

        assertSame(existing, result);
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

    private void setId(BaseLine baseLine, Long id) {
        setField(BaseLine.class, baseLine, "id", id);
    }

    private void setId(User user, Long id) {
        setField(User.class, user, "id", id);
    }

    private void setField(Class<?> owner, Object target, String fieldName, Object value) {
        try {
            Field field = owner.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to set test field " + fieldName, exception);
        }
    }
}
