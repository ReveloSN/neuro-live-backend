package com.neurolive.neuro_live_backend.business.patterns;

import com.neurolive.neuro_live_backend.data.enums.StateEnum;
import com.neurolive.neuro_live_backend.data.enums.TypeEnum;
import com.neurolive.neuro_live_backend.domain.biometric.ActivationThreshold;
import com.neurolive.neuro_live_backend.domain.biometric.BaseLine;
import com.neurolive.neuro_live_backend.domain.biometric.BiometricData;
import org.junit.jupiter.api.Test;

import java.time.Instant;
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
                Instant.parse("2026-04-01T14:00:00Z")
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
                Instant.parse("2026-04-01T14:05:00Z")
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
                Instant.parse("2026-04-01T14:10:00Z")
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
                Instant.parse("2026-04-01T14:15:00Z")
        );

        CrisisMediator.CrisisMediationResult result = crisisMediator.mediate(
                new CrisisMediator.CrisisEvaluationInput(44L, currentBiometricData, baseLine, null, 0.05f)
        );

        assertNotNull(result.crisisEvent());
        assertEquals(44L, result.crisisEvent().getPatientId());
        assertEquals(StateEnum.ACTIVE_CRISIS, result.crisisEvent().getState());
        assertEquals(LocalDateTime.ofInstant(currentBiometricData.timestamp(), java.time.ZoneId.systemDefault()), result.crisisEvent().getStartedAt());
        assertTrue(result.crisisEvent().isActive());
    }

    @Test
    void shouldNotOpenCrisisEventWhenStateIsNormal() {
        BaseLine baseLine = buildReadyBaseLine(45L, 82.0f, 98.0f);
        BiometricData currentBiometricData = new BiometricData(
                83.0f,
                98.0f,
                Instant.parse("2026-04-01T14:20:00Z")
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
                Instant.parse("2026-04-01T14:25:00Z")
        );

        CrisisMediator.CrisisMediationResult result = crisisMediator.mediate(
                new CrisisMediator.CrisisEvaluationInput(46L, currentBiometricData, baseLine, null, null)
        );

        assertNotNull(result.interventionProtocol());
        assertEquals(TypeEnum.BREATHING, result.interventionProtocol().getType());
        assertEquals(Boolean.FALSE, result.interventionProtocol().getActive());
        assertEquals(Boolean.TRUE, result.interventionProtocol().getBreathingEnabled());
        assertEquals(4, result.interventionProtocol().getBreathingRhythm());
        assertEquals(6, result.interventionProtocol().getBreathingCycles());
    }

    @Test
    void shouldDelegateToUiInterventionWhenTypingErrorRateTriggersCrisis() {
        BaseLine baseLine = buildReadyBaseLine(49L, 80.0f, 98.0f);
        BiometricData currentBiometricData = new BiometricData(
                84.0f,
                97.0f,
                Instant.parse("2026-04-01T14:40:00Z")
        );

        CrisisMediator.CrisisMediationResult result = crisisMediator.mediate(
                new CrisisMediator.CrisisEvaluationInput(49L, currentBiometricData, baseLine, null, 0.30f)
        );

        assertEquals(TypeEnum.UI, result.interventionProtocol().getType());
        assertEquals(Boolean.TRUE, result.interventionProtocol().getUiReductionEnabled());
    }

    @Test
    void shouldPreferExplicitThresholdWhenAvailable() {
        BaseLine baseLine = buildReadyBaseLine(47L, 80.0f, 98.0f);
        BiometricData currentBiometricData = new BiometricData(
                96.0f,
                97.0f,
                Instant.parse("2026-04-01T14:30:00Z")
        );
        ActivationThreshold activationThreshold = new ActivationThreshold(null, 90.0f, null, null);

        CrisisMediator.CrisisMediationResult result = crisisMediator.mediate(
                new CrisisMediator.CrisisEvaluationInput(47L, currentBiometricData, baseLine, activationThreshold, null)
        );

        assertTrue(result.emotionalState().isCrisis());
        assertTrue(result.crisisDetected());
    }

    @Test
    void shouldReturnNormalStateAndNoInterventionWhenBiometricsAreLow() {
        BaseLine baseLine = buildReadyBaseLine(49L, 70.0f, 99.0f);
        BiometricData biometricData = new BiometricData(72.0f, 99.0f,
                Instant.parse("2026-04-01T14:40:00Z"));

        CrisisMediator.CrisisMediationResult result = crisisMediator.mediate(
                new CrisisMediator.CrisisEvaluationInput(49L, biometricData, baseLine, null, null)
        );

        assertEquals(StateEnum.NORMAL, result.emotionalState().state());
        assertFalse(result.crisisDetected());
        assertNull(result.interventionProtocol());
    }

    @Test
    void shouldBehaveSafelyWhenThresholdIsAbsent() {
        BaseLine baseLine = buildReadyBaseLine(48L, 81.0f, 98.0f);
        BiometricData currentBiometricData = new BiometricData(
                99.0f,
                96.0f,
                Instant.parse("2026-04-01T14:35:00Z")
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
        Instant sessionStart = Instant.parse("2026-04-01T09:00:00Z");

        baseLine.calculate(List.of(
                new BiometricData(avgBpm, avgSpo2, sessionStart),
                new BiometricData(avgBpm, avgSpo2, sessionStart.plusSeconds(60)),
                new BiometricData(avgBpm, avgSpo2, sessionStart.plusSeconds(120)),
                new BiometricData(avgBpm, avgSpo2, sessionStart.plusSeconds(180)),
                new BiometricData(avgBpm, avgSpo2, sessionStart.plusSeconds(240)),
                new BiometricData(avgBpm, avgSpo2, sessionStart.plusSeconds(300))
        ));

        return baseLine;
    }

    private CrisisMediator buildMediator() {
        return new CrisisMediator(List.of(
                new UIIntervention(),
                new BreathingIntervention(),
                new LightIntervention(),
                new AudioIntervention()
        ));
    }
}
