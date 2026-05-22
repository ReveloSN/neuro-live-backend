package com.neurolive.neuro_live_backend.business.service;

import com.neurolive.neuro_live_backend.domain.biometric.BiometricTelemetrySample;
import com.neurolive.neuro_live_backend.domain.crisis.CrisisEvent;
import com.neurolive.neuro_live_backend.data.enums.StateEnum;
import com.neurolive.neuro_live_backend.data.enums.TypeEnum;
import com.neurolive.neuro_live_backend.repository.CrisisEventRepository;
import java.lang.reflect.Field;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Prueba los fallbacks seguros del servicio de insights clinicos.
class ClinicalInsightServiceTest {

    @Test
    void shouldReturnFallbackInsightWhenGeminiIsDisabled() {
        ClinicalInsightService service = new ClinicalInsightService(
                "",
                "gemini-2.5-flash-lite",
                false,
                15,
                mock(CrisisEventRepository.class)
        );

        String insight = service.generatePatientEvolutionInsight(
                91L,
                List.<CrisisEvent>of(),
                List.<BiometricTelemetrySample>of(),
                LocalDateTime.of(2026, 5, 1, 8, 0),
                LocalDateTime.of(2026, 5, 8, 8, 0)
        );

        assertThat(insight).contains("Insight narrativo no generado por IA");
        assertThat(insight).contains("0 eventos de crisis");
        assertThat(insight).contains("0 muestras biometricas");
    }

    @Test
    void shouldPersistFallbackClinicalSummaryWhenGeminiIsDisabled() {
        CrisisEventRepository crisisEventRepository = mock(CrisisEventRepository.class);
        ClinicalInsightService service = new ClinicalInsightService(
                "",
                "gemini-2.5-flash-lite",
                false,
                15,
                crisisEventRepository
        );
        CrisisEvent crisisEvent = CrisisEvent.open(
                92L,
                StateEnum.ACTIVE_CRISIS,
                LocalDateTime.of(2026, 5, 10, 10, 0)
        );
        crisisEvent.close(LocalDateTime.of(2026, 5, 10, 10, 3), StateEnum.NORMAL, TypeEnum.NO_INTERVENTION);
        setId(crisisEvent, 920L);
        when(crisisEventRepository.findById(920L)).thenReturn(Optional.of(crisisEvent));
        when(crisisEventRepository.save(crisisEvent)).thenReturn(crisisEvent);

        service.generatePostCrisisSummaryAsync(920L);

        assertThat(crisisEvent.getClinicalSummary()).contains("Resumen clinico no generado por IA");
        assertThat(crisisEvent.getClinicalSummaryGeneratedAt()).isNotNull();
        assertThat(crisisEvent.getClinicalSummaryModel()).isEqualTo("fallback");
        verify(crisisEventRepository).save(crisisEvent);
    }

    @Test
    void shouldSkipSummaryGenerationWhenCrisisEventIdIsNull() {
        CrisisEventRepository crisisEventRepository = mock(CrisisEventRepository.class);
        ClinicalInsightService service = new ClinicalInsightService(
                "",
                "gemini-2.5-flash-lite",
                false,
                15,
                crisisEventRepository
        );

        service.generatePostCrisisSummaryAsync(null);

        verify(crisisEventRepository, never()).findById(any());
    }

    @Test
    void shouldSkipSummaryGenerationWhenCrisisEventIdIsZero() {
        CrisisEventRepository crisisEventRepository = mock(CrisisEventRepository.class);
        ClinicalInsightService service = new ClinicalInsightService(
                "",
                "gemini-2.5-flash-lite",
                false,
                15,
                crisisEventRepository
        );

        service.generatePostCrisisSummaryAsync(0L);

        verify(crisisEventRepository, never()).findById(any());
    }

    @Test
    void shouldUseConfiguredGeminiTimeout() {
        ClinicalInsightService service = new ClinicalInsightService(
                "configured-key",
                "gemini-2.5-flash-lite",
                true,
                20,
                mock(CrisisEventRepository.class)
        );

        assertThat(readTimeout(service)).isEqualTo(Duration.ofSeconds(20));
    }

    // Asigna un ID persistido simulado para la prueba asincrona.
    private void setId(CrisisEvent crisisEvent, Long id) {
        try {
            Field field = CrisisEvent.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(crisisEvent, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to set test crisis id", exception);
        }
    }

    private Duration readTimeout(ClinicalInsightService service) {
        try {
            Field field = ClinicalInsightService.class.getDeclaredField("geminiTimeout");
            field.setAccessible(true);
            return (Duration) field.get(service);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to read Gemini timeout", exception);
        }
    }
}
