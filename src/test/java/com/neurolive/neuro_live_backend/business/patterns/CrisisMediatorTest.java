package com.neurolive.neuro_live_backend.business.patterns;

import com.neurolive.neuro_live_backend.data.enums.StateEnum;
import com.neurolive.neuro_live_backend.data.enums.TypeEnum;
import com.neurolive.neuro_live_backend.domain.biometric.ActivationThreshold;
import com.neurolive.neuro_live_backend.domain.biometric.BaseLine;
import com.neurolive.neuro_live_backend.domain.biometric.BiometricData;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrisisMediatorTest {

    private final CrisisMediator crisisMediator = buildMediator();

    @Test
    void shouldReturnNormalStateWhenValuesAreWithinRange() {
        BaseLine baseLine = buildReadyBaseLine(41L, 80.0f, 98.0f);
        BiometricData currentBiometricData = new BiometricData(
                84.0f,
                97.5f,
                LocalDateTime.of(2026, 4, 1, 14, 0)
        );

        CrisisMediator.CrisisMediationResult result = crisisMediator.mediate(
                new CrisisMediator.CrisisEvaluationInput(41L, currentBiometricData, baseLine, null, null)
        );

        assertTrue(result.emotionalState().isNormal());
        assertFalse(result.crisisDetected());
        assertFalse(result.interventionPrepared());
        assertNull(result.crisisEvent());
        assertNull(result.interventionProtocol());
    }

    @Test
    void shouldReturnAtRiskStateWhenValuesAreElevatedButBelowCrisisThreshold() {
        BaseLine baseLine = buildReadyBaseLine(42L, 80.0f, 98.0f);
        BiometricData currentBiometricData = new BiometricData(
                97.0f,
                96.0f,
                LocalDateTime.of(2026, 4, 1, 14, 5)
        );

        CrisisMediator.CrisisMediationResult result = crisisMediator.mediate(
                new CrisisMediator.CrisisEvaluationInput(42L, currentBiometricData, baseLine, null, null)
        );

        assertTrue(result.emotionalState().isAtRisk());
        assertFalse(result.crisisDetected());
        assertNull(result.crisisEvent());
    }

    @Test
    void shouldReturnCrisisStateWhenThresholdRulesIndicateCrisis() {
        BaseLine baseLine = buildReadyBaseLine(43L, 80.0f, 98.0f);
        BiometricData currentBiometricData = new BiometricData(
                113.0f,
                92.0f,
                LocalDateTime.of(2026, 4, 1, 14, 10)
        );

        CrisisMediator.CrisisMediationResult result = crisisMediator.mediate(
                new CrisisMediator.CrisisEvaluationInput(43L, currentBiometricData, baseLine, null, null)
        );

        assertTrue(result.emotionalState().isCrisis());
        assertTrue(result.crisisDetected());
        assertTrue(result.interventionPrepared());
    }

    @Test
    void shouldOpenCrisisEventWhenCrisisIsDetected() {
        BaseLine baseLine = buildReadyBaseLine(44L, 78.0f, 98.0f);
        BiometricData currentBiometricData = new BiometricData(
                110.0f,
                93.0f,
                LocalDateTime.of(2026, 4, 1, 14, 15)
        );

        CrisisMediator.CrisisMediationResult result = crisisMediator.mediate(
                new CrisisMediator.CrisisEvaluationInput(44L, currentBiometricData, baseLine, null, 0.05f)
        );

        assertNotNull(result.crisisEvent());
        assertEquals(44L, result.crisisEvent().getPatientId());
        assertEquals(StateEnum.ACTIVE_CRISIS, result.crisisEvent().getState());
        assertEquals(currentBiometricData.timestamp(), result.crisisEvent().getStartedAt());
        assertTrue(result.crisisEvent().isActive());
    }

    @Test
    void shouldNotOpenCrisisEventWhenStateIsNormal() {
        BaseLine baseLine = buildReadyBaseLine(45L, 82.0f, 98.0f);
        BiometricData currentBiometricData = new BiometricData(
                83.0f,
                98.0f,
                LocalDateTime.of(2026, 4, 1, 14, 20)
        );

        CrisisMediator.CrisisMediationResult result = crisisMediator.mediate(
                new CrisisMediator.CrisisEvaluationInput(45L, currentBiometricData, baseLine, null, 0.02f)
        );

        assertFalse(result.crisisDetected());
        assertNull(result.crisisEvent());
    }

    @Test
    void shouldPrepareInterventionProtocolWhenCrisisIsDetected() {
        BaseLine baseLine = buildReadyBaseLine(46L, 79.0f, 98.0f);
        BiometricData currentBiometricData = new BiometricData(
                106.0f,
                92.0f,
                LocalDateTime.of(2026, 4, 1, 14, 25)
        );

        CrisisMediator.CrisisMediationResult result = crisisMediator.mediate(
                new CrisisMediator.CrisisEvaluationInput(46L, currentBiometricData, baseLine, null, null)
        );

        assertNotNull(result.interventionProtocol());
        assertEquals(TypeEnum.GUIDED_BREATHING, result.interventionProtocol().getType());
        assertEquals(Boolean.FALSE, result.interventionProtocol().getActive());
        assertEquals(Boolean.TRUE, result.interventionProtocol().getBreathingEnabled());
    }

    @Test
    void shouldDelegateToUiReductionStrategyWhenTypingErrorRateTriggersCrisis() {
        BaseLine baseLine = buildReadyBaseLine(49L, 80.0f, 98.0f);
        BiometricData currentBiometricData = new BiometricData(
                84.0f,
                97.0f,
                LocalDateTime.of(2026, 4, 1, 14, 40)
        );

        CrisisMediator.CrisisMediationResult result = crisisMediator.mediate(
                new CrisisMediator.CrisisEvaluationInput(49L, currentBiometricData, baseLine, null, 0.30f)
        );

        assertEquals(TypeEnum.UI_REDUCTION, result.interventionProtocol().getType());
        assertEquals(Boolean.TRUE, result.interventionProtocol().getUiReductionEnabled());
    }

    @Test
    void shouldPreferExplicitThresholdWhenAvailable() {
        BaseLine baseLine = buildReadyBaseLine(47L, 80.0f, 98.0f);
        BiometricData currentBiometricData = new BiometricData(
                96.0f,
                97.0f,
                LocalDateTime.of(2026, 4, 1, 14, 30)
        );
        ActivationThreshold activationThreshold = new ActivationThreshold(null, 90.0f, null, null);

        CrisisMediator.CrisisMediationResult result = crisisMediator.mediate(
                new CrisisMediator.CrisisEvaluationInput(47L, currentBiometricData, baseLine, activationThreshold, null)
        );

        assertTrue(result.emotionalState().isCrisis());
        assertTrue(result.crisisDetected());
    }

    @Test
    void shouldBehaveSafelyWhenThresholdIsAbsent() {
        BaseLine baseLine = buildReadyBaseLine(48L, 81.0f, 98.0f);
        BiometricData currentBiometricData = new BiometricData(
                99.0f,
                96.0f,
                LocalDateTime.of(2026, 4, 1, 14, 35)
        );

        CrisisMediator.CrisisMediationResult result = crisisMediator.mediate(
                new CrisisMediator.CrisisEvaluationInput(48L, currentBiometricData, baseLine, null, 0.10f)
        );

        assertTrue(result.emotionalState().isAtRisk());
        assertFalse(result.crisisDetected());
        assertFalse(result.interventionPrepared());
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

    private CrisisMediator buildMediator() {
        return new CrisisMediator(List.of(
                new UiReductionStrategy(),
                new GuidedBreathingStrategy(),
                new LightingInterventionStrategy(),
                new AuditoryRegulationStrategy()
        ));
    }
}
