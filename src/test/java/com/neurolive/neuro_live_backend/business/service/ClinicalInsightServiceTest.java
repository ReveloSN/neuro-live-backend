package com.neurolive.neuro_live_backend.business.service;

import com.neurolive.neuro_live_backend.domain.biometric.BiometricTelemetrySample;
import com.neurolive.neuro_live_backend.domain.crisis.CrisisEvent;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// Prueba los fallbacks seguros del servicio de insights clinicos.
class ClinicalInsightServiceTest {

    @Test
    void shouldReturnFallbackInsightWhenGeminiIsDisabled() {
        ClinicalInsightService service = new ClinicalInsightService("", "gemini-2.5-flash-lite", false);

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
}
