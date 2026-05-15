package com.neurolive.neuro_live_backend.business.service;

import com.neurolive.neuro_live_backend.domain.biometric.BiometricData;
import com.neurolive.neuro_live_backend.domain.biometric.BiometricTelemetrySample;
import com.neurolive.neuro_live_backend.domain.user.Doctor;
import com.neurolive.neuro_live_backend.domain.user.User;
import com.neurolive.neuro_live_backend.repository.BiometricTelemetrySampleRepository;
import com.neurolive.neuro_live_backend.repository.CrisisEventRepository;
import com.neurolive.neuro_live_backend.repository.SAMResponseRepository;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
// Prueba la construccion del insight narrativo desde CrisisService.
class CrisisServiceTest {

    private final CrisisEventRepository crisisEventRepository = mock(CrisisEventRepository.class);
    private final SAMResponseRepository samResponseRepository = mock(SAMResponseRepository.class);
    private final ClinicalAccessService clinicalAccessService = mock(ClinicalAccessService.class);
    private final AuditLogService auditLogService = mock(AuditLogService.class);
    private final BaseLineService baseLineService = mock(BaseLineService.class);
    private final BiometricTelemetrySampleRepository biometricTelemetrySampleRepository =
            mock(BiometricTelemetrySampleRepository.class);
    private final ClinicalInsightService clinicalInsightService = mock(ClinicalInsightService.class);

    private final CrisisService crisisService = new CrisisService(
            crisisEventRepository,
            samResponseRepository,
            clinicalAccessService,
            auditLogService,
            baseLineService,
            biometricTelemetrySampleRepository,
            clinicalInsightService
    );

    @Test
    void shouldBuildNarrativeInsightWithAuthorizationAndAudit() {
        Doctor doctor = new Doctor();
        doctor.register("Doctor Test", "doctor@neurolive.test", "encoded-secret");
        setId(doctor, 701L);
        LocalDateTime observedAt = LocalDateTime.now().minusDays(1);
        BiometricTelemetrySample sample = BiometricTelemetrySample.from(
                44L,
                "AA:BB:CC:DD:EE:44",
                new BiometricData(91.0f, 97.0f, observedAt)
        );

        when(clinicalAccessService.requirePatientAccess("doctor@neurolive.test", 44L)).thenReturn(doctor);
        when(crisisEventRepository.findAllByPatientIdAndStartedAtBetweenOrderByStartedAtDesc(
                eq(44L),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(List.of());
        when(biometricTelemetrySampleRepository.findAllByPatientIdAndObservedAtBetweenOrderByObservedAtAsc(
                eq(44L),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(List.of(sample));
        when(clinicalInsightService.generatePatientEvolutionInsight(
                eq(44L),
                eq(List.of()),
                anyList(),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn("Narrativa clinica de prueba");

        CrisisService.ClinicalInsightSnapshot snapshot = crisisService.buildNarrativeInsight(
                "doctor@neurolive.test",
                44L,
                7,
                "127.0.0.1"
        );

        assertThat(snapshot.patientId()).isEqualTo(44L);
        assertThat(snapshot.insight()).isEqualTo("Narrativa clinica de prueba");
        verify(auditLogService).record(701L, "READ_CLINICAL_AI_INSIGHT", 44L, "127.0.0.1");

        ArgumentCaptor<List<BiometricTelemetrySample>> telemetryCaptor = ArgumentCaptor.forClass(List.class);
        verify(clinicalInsightService).generatePatientEvolutionInsight(
                eq(44L),
                eq(List.of()),
                telemetryCaptor.capture(),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        );
        assertThat(telemetryCaptor.getValue()).containsExactly(sample);
    }

    @Test
    void shouldReturnFallbackInsightFromServiceForEndpointWhenGeminiIsDisabled() {
        Doctor doctor = new Doctor();
        doctor.register("Doctor Fallback", "doctor.fallback@neurolive.test", "encoded-secret");
        setId(doctor, 702L);
        when(clinicalAccessService.requirePatientAccess("doctor.fallback@neurolive.test", 45L)).thenReturn(doctor);
        when(crisisEventRepository.findAllByPatientIdAndStartedAtBetweenOrderByStartedAtDesc(
                eq(45L),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(List.of());
        when(biometricTelemetrySampleRepository.findAllByPatientIdAndObservedAtBetweenOrderByObservedAtAsc(
                eq(45L),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(List.of());
        when(clinicalInsightService.generatePatientEvolutionInsight(
                eq(45L),
                eq(List.of()),
                eq(List.of()),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn("Insight narrativo no generado por IA.");

        CrisisService.ClinicalInsightSnapshot snapshot = crisisService.buildNarrativeInsight(
                "doctor.fallback@neurolive.test",
                45L,
                7,
                "127.0.0.1"
        );

        assertThat(snapshot.insight()).contains("Insight narrativo no generado por IA");
        verify(auditLogService).record(702L, "READ_CLINICAL_AI_INSIGHT", 45L, "127.0.0.1");
    }

    // Asigna IDs a entidades del dominio usadas en auditoria.
    private void setId(User user, Long id) {
        try {
            Field field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to set test user id", exception);
        }
    }
}
