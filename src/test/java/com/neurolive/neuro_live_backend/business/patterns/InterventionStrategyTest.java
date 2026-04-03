package com.neurolive.neuro_live_backend.business.patterns;

import com.neurolive.neuro_live_backend.data.enums.TypeEnum;
import com.neurolive.neuro_live_backend.domain.biometric.ActivationThreshold;
import com.neurolive.neuro_live_backend.domain.biometric.BaseLine;
import com.neurolive.neuro_live_backend.domain.biometric.BiometricData;
import com.neurolive.neuro_live_backend.domain.crisis.InterventionProtocol;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InterventionStrategyTest {

    @Test
    void shouldPrepareGuidedBreathingProtocolThroughStrategy() {
        GuidedBreathingStrategy strategy = new GuidedBreathingStrategy();

        InterventionProtocol interventionProtocol = strategy.prepareProtocol(
                new CrisisMediator.CrisisEvaluationInput(
                        51L,
                        new BiometricData(105.0f, 92.0f, LocalDateTime.of(2026, 4, 1, 15, 0)),
                        buildReadyBaseLine(51L, 80.0f, 98.0f),
                        null,
                        null
                )
        );

        assertEquals(TypeEnum.GUIDED_BREATHING, interventionProtocol.getType());
        assertEquals(Boolean.TRUE, interventionProtocol.getBreathingEnabled());
    }

    @Test
    void shouldPrepareLightingProtocolThroughStrategy() {
        LightingInterventionStrategy strategy = new LightingInterventionStrategy();

        InterventionProtocol interventionProtocol = strategy.prepareProtocol(
                new CrisisMediator.CrisisEvaluationInput(
                        52L,
                        new BiometricData(104.0f, 97.0f, LocalDateTime.of(2026, 4, 1, 15, 5)),
                        buildReadyBaseLine(52L, 80.0f, 98.0f),
                        new ActivationThreshold(null, 90.0f, null, null),
                        null
                )
        );

        assertEquals(TypeEnum.LIGHTING_CONTROL, interventionProtocol.getType());
        assertEquals("blue", interventionProtocol.getLightColor());
        assertEquals(55, interventionProtocol.getLightIntensity());
    }

    @Test
    void shouldPrepareAuditoryRegulationProtocolThroughStrategy() {
        AuditoryRegulationStrategy strategy = new AuditoryRegulationStrategy();

        InterventionProtocol interventionProtocol = strategy.prepareProtocol(
                new CrisisMediator.CrisisEvaluationInput(
                        53L,
                        new BiometricData(113.0f, 98.0f, LocalDateTime.of(2026, 4, 1, 15, 10)),
                        buildReadyBaseLine(53L, 80.0f, 98.0f),
                        null,
                        null
                )
        );

        assertEquals(TypeEnum.AUDITORY_REGULATION, interventionProtocol.getType());
        assertEquals("calm-default", interventionProtocol.getAudioTrack());
    }

    @Test
    void shouldPrepareUiReductionProtocolThroughStrategy() {
        UiReductionStrategy strategy = new UiReductionStrategy();

        InterventionProtocol interventionProtocol = strategy.prepareProtocol(
                new CrisisMediator.CrisisEvaluationInput(
                        54L,
                        new BiometricData(85.0f, 97.0f, LocalDateTime.of(2026, 4, 1, 15, 15)),
                        buildReadyBaseLine(54L, 80.0f, 98.0f),
                        null,
                        0.30f
                )
        );

        assertEquals(TypeEnum.UI_REDUCTION, interventionProtocol.getType());
        assertEquals(Boolean.TRUE, interventionProtocol.getUiReductionEnabled());
    }

    @Test
    void shouldAllowCrisisMediatorToDelegateToAStrategy() {
        CrisisMediator crisisMediator = new CrisisMediator(List.of(
                new UiReductionStrategy(),
                new GuidedBreathingStrategy(),
                new LightingInterventionStrategy(),
                new AuditoryRegulationStrategy()
        ));

        CrisisMediator.CrisisMediationResult result = crisisMediator.mediate(
                new CrisisMediator.CrisisEvaluationInput(
                        55L,
                        new BiometricData(115.0f, 98.0f, LocalDateTime.of(2026, 4, 1, 15, 20)),
                        buildReadyBaseLine(55L, 80.0f, 98.0f),
                        null,
                        null
                )
        );

        assertEquals(TypeEnum.AUDITORY_REGULATION, result.interventionProtocol().getType());
    }

    @Test
    void shouldKeepCurrentCrisisDomainModelStable() {
        GuidedBreathingStrategy strategy = new GuidedBreathingStrategy();

        InterventionProtocol interventionProtocol = strategy.prepareProtocol(
                new CrisisMediator.CrisisEvaluationInput(
                        56L,
                        new BiometricData(104.0f, 92.0f, LocalDateTime.of(2026, 4, 1, 15, 25)),
                        buildReadyBaseLine(56L, 80.0f, 98.0f),
                        null,
                        null
                )
        );

        assertFalse(interventionProtocol.getActive());
        assertTrue(interventionProtocol.getCrisisEvent() == null);
    }

    private BaseLine buildReadyBaseLine(Long patientId, float avgBpm, float avgSpo2) {
        BaseLine baseLine = new BaseLine(patientId);
        LocalDateTime sessionStart = LocalDateTime.of(2026, 4, 1, 9, 0);

        baseLine.calculate(List.of(
                new BiometricData(avgBpm, avgSpo2, sessionStart),
                new BiometricData(avgBpm, avgSpo2, sessionStart.plusMinutes(1)),
                new BiometricData(avgBpm, avgSpo2, sessionStart.plusMinutes(2)),
                new BiometricData(avgBpm, avgSpo2, sessionStart.plusMinutes(3)),
                new BiometricData(avgBpm, avgSpo2, sessionStart.plusMinutes(4)),
                new BiometricData(avgBpm, avgSpo2, sessionStart.plusMinutes(5))
        ));

        return baseLine;
    }
}
