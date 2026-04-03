package com.neurolive.neuro_live_backend.business.patterns;

import com.neurolive.neuro_live_backend.domain.biometric.BaseLine;
import com.neurolive.neuro_live_backend.domain.biometric.BiometricData;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PatientStateObserverTest {

    @Test
    void shouldSubscribeObserverCorrectly() {
        RecordingObserver observer = new RecordingObserver();
        CrisisMediator crisisMediator = buildMediator();

        assertTrue(crisisMediator.subscribe(observer));

        crisisMediator.mediate(buildNormalInput(61L));

        assertEquals(1, observer.updates().size());
    }

    @Test
    void shouldUnsubscribeObserverCorrectly() {
        RecordingObserver observer = new RecordingObserver();
        CrisisMediator crisisMediator = buildMediator();

        crisisMediator.subscribe(observer);
        assertTrue(crisisMediator.unsubscribe(observer));

        crisisMediator.mediate(buildNormalInput(62L));

        assertTrue(observer.updates().isEmpty());
    }

    @Test
    void shouldNotifyAllSubscribedObserversOnStateChange() {
        RecordingObserver firstObserver = new RecordingObserver();
        RecordingObserver secondObserver = new RecordingObserver();
        CrisisMediator crisisMediator = buildMediator();

        crisisMediator.subscribe(firstObserver);
        crisisMediator.subscribe(secondObserver);

        crisisMediator.mediate(buildAtRiskInput(63L));

        assertEquals(1, firstObserver.updates().size());
        assertEquals(1, secondObserver.updates().size());
    }

    @Test
    void shouldPassTheExpectedUpdatePayload() {
        RecordingObserver observer = new RecordingObserver();
        CrisisMediator crisisMediator = buildMediator();
        CrisisMediator.CrisisEvaluationInput input = buildCrisisInput(64L);

        crisisMediator.subscribe(observer);
        crisisMediator.mediate(input);

        PatientStateUpdate update = observer.updates().getFirst();
        assertEquals(64L, update.patientId());
        assertTrue(update.emotionalState().isCrisis());
        assertTrue(update.crisisDetected());
        assertTrue(update.interventionPrepared());
        assertTrue(update.shouldNotifyCaregiver());
        assertTrue(update.shouldNotifyDoctor());
        assertEquals(input.currentBiometricData().timestamp(), update.observedAt());
    }

    @Test
    void shouldNotFailWhenNoObserversAreSubscribed() {
        CrisisMediator crisisMediator = buildMediator();

        assertDoesNotThrow(() -> crisisMediator.mediate(buildNormalInput(65L)));
    }

    private CrisisMediator buildMediator() {
        return new CrisisMediator(List.of(
                new UiReductionStrategy(),
                new GuidedBreathingStrategy(),
                new LightingInterventionStrategy(),
                new AuditoryRegulationStrategy()
        ));
    }

    private CrisisMediator.CrisisEvaluationInput buildNormalInput(Long patientId) {
        BaseLine baseLine = buildReadyBaseLine(patientId, 80.0f, 98.0f);
        return new CrisisMediator.CrisisEvaluationInput(
                patientId,
                new BiometricData(82.0f, 98.0f, LocalDateTime.of(2026, 4, 1, 16, 0)),
                baseLine,
                null,
                null);
    }

    private CrisisMediator.CrisisEvaluationInput buildAtRiskInput(Long patientId) {
        BaseLine baseLine = buildReadyBaseLine(patientId, 80.0f, 98.0f);
        return new CrisisMediator.CrisisEvaluationInput(
                patientId,
                new BiometricData(96.0f, 96.0f, LocalDateTime.of(2026, 4, 1, 16, 5)),
                baseLine,
                null,
                null);
    }

    private CrisisMediator.CrisisEvaluationInput buildCrisisInput(Long patientId) {
        BaseLine baseLine = buildReadyBaseLine(patientId, 80.0f, 98.0f);
        return new CrisisMediator.CrisisEvaluationInput(
                patientId,
                new BiometricData(84.0f, 97.0f, LocalDateTime.of(2026, 4, 1, 16, 10)),
                baseLine,
                null,
                0.30f);
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

    private static final class RecordingObserver implements PatientStateObserver {

        private final List<PatientStateUpdate> updates = new ArrayList<>();

        @Override
        public void onPatientStateChanged(PatientStateUpdate update) {
            updates.add(update);
        }

        private List<PatientStateUpdate> updates() {
            return updates;
        }
    }
}
