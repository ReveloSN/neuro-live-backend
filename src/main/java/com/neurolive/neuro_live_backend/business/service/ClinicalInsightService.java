package com.neurolive.neuro_live_backend.business.service;

import com.neurolive.neuro_live_backend.domain.biometric.BiometricTelemetrySample;
import com.neurolive.neuro_live_backend.domain.crisis.CrisisEvent;
import com.neurolive.neuro_live_backend.repository.CrisisEventRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

@Service
// Genera textos clinicos usando solo contexto anonimizado.
public class ClinicalInsightService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClinicalInsightService.class);
    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta";

    private final WebClient geminiClient;
    private final String apiKey;
    private final String model;
    private final boolean enabled;
    private final CrisisEventRepository crisisEventRepository;

    public ClinicalInsightService(@Value("${gemini.api-key:}") String apiKey,
                                  @Value("${gemini.model:gemini-2.5-flash-lite}") String model,
                                  @Value("${gemini.enabled:false}") boolean enabled,
                                  CrisisEventRepository crisisEventRepository) {
        this.geminiClient = WebClient.builder().baseUrl(GEMINI_BASE_URL).build();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model == null || model.isBlank() ? "gemini-2.5-flash-lite" : model.trim();
        this.enabled = enabled && !this.apiKey.isBlank();
        this.crisisEventRepository = crisisEventRepository;
        if (this.enabled) {
            LOGGER.info("Clinical Gemini insight enabled model={}", this.model);
        } else {
            LOGGER.info("Clinical Gemini insight disabled; GEMINI_API_KEY is empty or GEMINI_ENABLED is false");
        }
    }

    @Async
    @Transactional
    // Genera y persiste un resumen post-crisis sin bloquear el cierre.
    public void generatePostCrisisSummaryAsync(Long crisisEventId) {
        if (crisisEventId == null || crisisEventId <= 0) {
            return;
        }
        crisisEventRepository.findById(crisisEventId).ifPresent(crisisEvent -> {
            SummaryResult summary = generatePostCrisisSummary(crisisEvent);
            crisisEvent.recordClinicalSummary(summary.text(), LocalDateTime.now(), summary.model());
            crisisEventRepository.save(crisisEvent);
            LOGGER.info("Clinical summary persisted crisisId={} model={}", crisisEvent.getId(), summary.model());
        });
    }

    @Transactional(readOnly = true)
    // Construye narrativa reciente para dashboards clinicos.
    public String generatePatientEvolutionInsight(Long patientId,
                                                  List<CrisisEvent> crisisEvents,
                                                  List<BiometricTelemetrySample> telemetrySamples,
                                                  LocalDateTime start,
                                                  LocalDateTime end) {
        if (!enabled) {
            return fallbackPatientInsight(crisisEvents, telemetrySamples);
        }
        try {
            String prompt = buildPatientInsightPrompt(crisisEvents, telemetrySamples, start, end);
            return callGemini(prompt, fallbackPatientInsight(crisisEvents, telemetrySamples));
        } catch (Exception exception) {
            LOGGER.warn("Clinical insight fallback patientId={} reason={}", patientId, exception.getMessage());
            return fallbackPatientInsight(crisisEvents, telemetrySamples);
        }
    }

    // Genera un resumen corto de una crisis cerrada.
    private SummaryResult generatePostCrisisSummary(CrisisEvent crisisEvent) {
        String fallback = fallbackCrisisSummary(crisisEvent);
        if (!enabled) {
            return new SummaryResult(fallback, "fallback");
        }
        try {
            String summary = callGemini(buildCrisisSummaryPrompt(crisisEvent), fallback);
            String sourceModel = summary.equals(fallback) ? "fallback" : model;
            return new SummaryResult(summary, sourceModel);
        } catch (Exception exception) {
            LOGGER.warn("Post-crisis summary fallback crisisId={} reason={}", crisisEvent.getId(), exception.getMessage());
            return new SummaryResult(fallback, "fallback");
        }
    }

    // Llama a Gemini con timeout corto y respuesta textual.
    private String callGemini(String prompt, String fallback) {
        Map<String, Object> request = Map.of(
                "contents", List.of(Map.of(
                        "parts", List.of(Map.of("text", prompt))
                )),
                "generationConfig", Map.of(
                        "temperature", 0.2,
                        "maxOutputTokens", 220
                )
        );

        Map<String, Object> response = geminiClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/models/{model}:generateContent")
                        .queryParam("key", apiKey)
                        .build(model))
                .bodyValue(request)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .block(Duration.ofSeconds(5));

        String text = extractText(response);
        return text == null || text.isBlank() ? fallback : text.trim();
    }

    // Prompt minimo para resumen posterior a crisis.
    private String buildCrisisSummaryPrompt(CrisisEvent crisisEvent) {
        return """
                Write a concise clinical post-crisis summary in Spanish.
                Do not include names, emails, account data, or identifying details.
                Use only this operational context:
                state=%s, durationSeconds=%d, triggerBpm=%s, triggerSpo2=%s,
                typingErrorRate=%s, interventionType=%s, samValence=%s, samArousal=%s.
                Include: likely pattern, response observed, and one follow-up suggestion.
                """.formatted(
                crisisEvent.getState(),
                crisisEvent.calculateDuration().getSeconds(),
                crisisEvent.getTriggerBpm(),
                crisisEvent.getTriggerSpo2(),
                crisisEvent.getTypingErrorRate(),
                crisisEvent.getInterventionType(),
                crisisEvent.getSamValence(),
                crisisEvent.getSamArousal()
        );
    }

    // Prompt minimo para evolucion clinica reciente.
    private String buildPatientInsightPrompt(List<CrisisEvent> crisisEvents,
                                             List<BiometricTelemetrySample> telemetrySamples,
                                             LocalDateTime start,
                                             LocalDateTime end) {
        double averageBpm = telemetrySamples.stream().mapToDouble(BiometricTelemetrySample::getBpm).average().orElse(0.0d);
        double averageSpo2 = telemetrySamples.stream().mapToDouble(BiometricTelemetrySample::getSpo2).average().orElse(0.0d);
        long activeCount = crisisEvents.stream().filter(CrisisEvent::isActive).count();

        return """
                Write a short narrative clinical insight in Spanish for a doctor dashboard.
                Avoid personal identifiers. Use only aggregated operational data.
                Period start=%s, end=%s, crisisCount=%d, activeCrises=%d,
                telemetrySamples=%d, averageBpm=%.1f, averageSpo2=%.1f.
                Describe trend, risk interpretation, and one monitoring recommendation.
                """.formatted(
                start,
                end,
                crisisEvents.size(),
                activeCount,
                telemetrySamples.size(),
                averageBpm,
                averageSpo2
        );
    }

    // Extrae texto de la respuesta generativa.
    @SuppressWarnings("unchecked")
    private String extractText(Map<String, Object> response) {
        if (response == null) {
            return null;
        }
        Object candidatesObject = response.get("candidates");
        if (!(candidatesObject instanceof List<?> candidates) || candidates.isEmpty()) {
            return null;
        }
        Object firstCandidate = candidates.getFirst();
        if (!(firstCandidate instanceof Map<?, ?> candidate)) {
            return null;
        }
        Object contentObject = candidate.get("content");
        if (!(contentObject instanceof Map<?, ?> content)) {
            return null;
        }
        Object partsObject = content.get("parts");
        if (!(partsObject instanceof List<?> parts) || parts.isEmpty()) {
            return null;
        }
        Object firstPart = parts.getFirst();
        if (!(firstPart instanceof Map<?, ?> part)) {
            return null;
        }
        Object text = part.get("text");
        return text == null ? null : text.toString();
    }

    // Texto seguro si Gemini no esta disponible.
    private String fallbackCrisisSummary(CrisisEvent crisisEvent) {
        return "Resumen clinico no generado por IA. Crisis cerrada con estado "
                + crisisEvent.getState()
                + " y duracion aproximada de "
                + crisisEvent.calculateDuration().getSeconds()
                + " segundos.";
    }

    // Texto seguro para dashboard cuando no hay IA.
    private String fallbackPatientInsight(List<CrisisEvent> crisisEvents,
                                          List<BiometricTelemetrySample> telemetrySamples) {
        return "Insight narrativo no generado por IA. En la ventana consultada hay "
                + crisisEvents.size()
                + " eventos de crisis y "
                + telemetrySamples.size()
                + " muestras biometricas disponibles para revision clinica.";
    }

    private record SummaryResult(String text, String model) {
    }
}
