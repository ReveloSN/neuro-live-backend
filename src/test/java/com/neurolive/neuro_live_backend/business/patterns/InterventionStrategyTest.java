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
    void shouldPrepareBreathingProtocolThroughStrategy() {
        BreathingIntervention strategy = new BreathingIntervention();

        InterventionProtocol interventionProtocol = strategy.prepareProtocol(
                new CrisisMediator.CrisisEvaluationInput(
                        51L,
                        new BiometricData(105.0f, 92.0f, LocalDateTime.of(2026, 4, 1, 15, 0)),
                        buildReadyBaseLine(51L, 80.0f, 98.0f),
                        null,
                        null
                )
        );

        assertEquals(TypeEnum.BREATHING, interventionProtocol.getType());
        assertEquals(Boolean.TRUE, interventionProtocol.getBreathingEnabled());
        assertEquals(4, interventionProtocol.getBreathingRhythm());
        assertEquals(6, interventionProtocol.getBreathingCycles());
    }

    @Test
    void shouldPrepareLightingProtocolThroughStrategy() {
        LightIntervention strategy = new LightIntervention();

        InterventionProtocol interventionProtocol = strategy.prepareProtocol(
                new CrisisMediator.CrisisEvaluationInput(
                        52L,
                        new BiometricData(104.0f, 97.0f, LocalDateTime.of(2026, 4, 1, 15, 5)),
                        buildReadyBaseLine(52L, 80.0f, 98.0f),
                        new ActivationThreshold(null, 90.0f, null, null),
                        null
                )
        );

        assertEquals(TypeEnum.LIGHT, interventionProtocol.getType());
        assertEquals("blue", interventionProtocol.getLightColor());
        assertEquals(55, interventionProtocol.getLightIntensity());
    }

    @Test
    void shouldPrepareAuditoryRegulationProtocolThroughStrategy() {
        AudioIntervention strategy = new AudioIntervention();

        InterventionProtocol interventionProtocol = strategy.prepareProtocol(
                new CrisisMediator.CrisisEvaluationInput(
                        53L,
                        new BiometricData(113.0f, 98.0f, LocalDateTime.of(2026, 4, 1, 15, 10)),
                        buildReadyBaseLine(53L, 80.0f, 98.0f),
                        null,
                        null
                )
        );

        assertEquals(TypeEnum.AUDIO, interventionProtocol.getType());
        assertEquals("calm-default", interventionProtocol.getAudioTrack());
        assertEquals(35, interventionProtocol.getAudioVolume());
    }

    @Test
    void shouldPrepareUiProtocolThroughStrategy() {
        UIIntervention strategy = new UIIntervention();

        InterventionProtocol interventionProtocol = strategy.prepareProtocol(
                new CrisisMediator.CrisisEvaluationInput(
                        54L,
                        new BiometricData(85.0f, 97.0f, LocalDateTime.of(2026, 4, 1, 15, 15)),
                        buildReadyBaseLine(54L, 80.0f, 98.0f),
                        null,
                        0.30f
                )
        );

        assertEquals(TypeEnum.UI, interventionProtocol.getType());
        assertEquals(Boolean.TRUE, interventionProtocol.getUiReductionEnabled());
        assertEquals("calm-focus", interventionProtocol.getUiTheme());
        assertEquals(Boolean.TRUE, interventionProtocol.getHighContrastEnabled());
    }

    @Test
    void shouldAllowCrisisMediatorToDelegateToAStrategy() {
        CrisisMediator crisisMediator = new CrisisMediator(List.of(
                new UIIntervention(),
                new BreathingIntervention(),
                new LightIntervention(),
                new AudioIntervention()
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

        assertEquals(TypeEnum.AUDIO, result.interventionProtocol().getType());
    }

    @Test
    void shouldKeepDeprecatedWrappersDelegatingToRenamedStrategies() {
        InterventionProtocol uiProtocol = new UiReductionStrategy().prepareProtocol(
                new CrisisMediator.CrisisEvaluationInput(
                        57L,
                        new BiometricData(85.0f, 97.0f, LocalDateTime.of(2026, 4, 1, 15, 22)),
                        buildReadyBaseLine(57L, 80.0f, 98.0f),
                        null,
                        0.30f
                )
        );
        InterventionProtocol breathingProtocol = new GuidedBreathingStrategy().prepareProtocol(
                new CrisisMediator.CrisisEvaluationInput(
                        58L,
                        new BiometricData(105.0f, 92.0f, LocalDateTime.of(2026, 4, 1, 15, 23)),
                        buildReadyBaseLine(58L, 80.0f, 98.0f),
                        null,
                        null
                )
        );

        assertEquals(TypeEnum.UI, uiProtocol.getType());
        assertEquals(TypeEnum.BREATHING, breathingProtocol.getType());
    }

    @Test
    void shouldKeepCurrentCrisisDomainModelStable() {
        BreathingIntervention strategy = new BreathingIntervention();

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
