package com.neurolive.neuro_live_backend.business.patterns;

import com.neurolive.neuro_live_backend.data.enums.StateEnum;
import com.neurolive.neuro_live_backend.domain.biometric.ActivationThreshold;
import com.neurolive.neuro_live_backend.domain.biometric.BaseLine;
import com.neurolive.neuro_live_backend.domain.biometric.BiometricData;
import com.neurolive.neuro_live_backend.domain.crisis.CrisisEvent;
import com.neurolive.neuro_live_backend.domain.crisis.EmotionalState;
import com.neurolive.neuro_live_backend.domain.crisis.InterventionProtocol;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@Service
// Centraliza la evaluacion de crisis en un solo punto
public class CrisisMediator {

    private static final float BPM_AT_RISK_DELTA = 15.0f;
    private static final float BPM_CRISIS_DELTA = 30.0f;
    private static final float SPO2_AT_RISK_DROP = 3.0f;
    private static final float SPO2_CRISIS_DROP = 5.0f;
    private static final float ERROR_RATE_AT_RISK = 0.15f;
    private static final float ERROR_RATE_CRISIS = 0.25f;
    private static final float BPM_THRESHOLD_MARGIN = 10.0f;
    private static final float SPO2_THRESHOLD_MARGIN = 2.0f;
    private static final float ERROR_RATE_THRESHOLD_MARGIN = 0.05f;

    private final List<InterventionStrategy> interventionStrategies;
    private final Set<PatientStateObserver> observers = new CopyOnWriteArraySet<>();

    @Autowired
    public CrisisMediator(List<InterventionStrategy> interventionStrategies,
                        List<PatientStateObserver> initialObservers) {
        if (interventionStrategies == null || interventionStrategies.isEmpty()) {
            throw new IllegalArgumentException("At least one intervention strategy is required");
        }
        this.interventionStrategies = List.copyOf(interventionStrategies);
        if (initialObservers != null) {
            observers.addAll(initialObservers);
        }
    }

    public CrisisMediator(List<InterventionStrategy> interventionStrategies) {
        this(interventionStrategies, List.of());
    }

    public boolean subscribe(PatientStateObserver observer) {
        if (observer == null) {
            throw new IllegalArgumentException("Patient state observer is required");
        }
        return observers.add(observer);
    }

    public boolean unsubscribe(PatientStateObserver observer) {
        if (observer == null) {
            throw new IllegalArgumentException("Patient state observer is required");
        }
        return observers.remove(observer);
    }

    public CrisisMediationResult mediate(CrisisEvaluationInput input) {
        if (input == null) {
            throw new IllegalArgumentException("Crisis evaluation input is required");
        }

        StateEnum resultingState = evaluateState(input);
        EmotionalState emotionalState = EmotionalState.from(resultingState);
        CrisisMediationResult result;

        if (resultingState != StateEnum.ACTIVE_CRISIS) {
            result = CrisisMediationResult.withoutCrisis(emotionalState);
            publishUpdate(buildPatientStateUpdate(input, result));
            return result;
        }

        CrisisEvent crisisEvent = CrisisEvent.open(
                input.patientId(),
                StateEnum.ACTIVE_CRISIS,
                input.currentBiometricData().timestamp()
        );
        crisisEvent.recordTriggerMetrics(
                input.currentBiometricData().bpm(),
                input.currentBiometricData().spo2(),
                input.typingErrorRate(),
                input.dwellTime(),
                input.flightTime(),
                input.typingErrorCount()
        );

        result = CrisisMediationResult.crisisDetected(
                emotionalState,
                crisisEvent,
                prepareInitialIntervention(input)
        );
        publishUpdate(buildPatientStateUpdate(input, result));
        return result;
    }

    public void publishUpdate(PatientStateUpdate update) {
        if (update == null) {
            throw new IllegalArgumentException("Patient state update is required");
        }
        notifyObservers(update);
    }

    private StateEnum evaluateState(CrisisEvaluationInput input) {
        StateEnum inferredState = hasUsableThreshold(input.activationThreshold())
                ? evaluateWithThreshold(input)
                : evaluateWithBaseline(input);
        return maxSeverity(inferredState, input.analysisStateHint());
    }

    private StateEnum evaluateWithThreshold(CrisisEvaluationInput input) {
        ActivationThreshold threshold = input.activationThreshold();
        validateThresholdRange(threshold);

        if (exceedsThreshold(input, threshold)) {
            return StateEnum.ACTIVE_CRISIS;
        }
        if (isNearThreshold(input, threshold)) {
            return StateEnum.RISK_ELEVATED;
        }
        return StateEnum.NORMAL;
    }

    private StateEnum evaluateWithBaseline(CrisisEvaluationInput input) {
        float bpmDelta = input.currentBiometricData().bpm() - input.baseLine().getAvgBpm();
        float spo2Drop = input.baseLine().getAvgSpo2() - input.currentBiometricData().spo2();
        Float typingErrorRate = input.typingErrorRate();

        if (bpmDelta >= BPM_CRISIS_DELTA
                || spo2Drop >= SPO2_CRISIS_DROP
                || isTypingErrorInCrisisRange(typingErrorRate)) {
            return StateEnum.ACTIVE_CRISIS;
        }
        if (bpmDelta >= BPM_AT_RISK_DELTA
                || spo2Drop >= SPO2_AT_RISK_DROP
                || isTypingErrorInRiskRange(typingErrorRate)) {
            return StateEnum.RISK_ELEVATED;
        }
        return StateEnum.NORMAL;
    }

    private InterventionProtocol prepareInitialIntervention(CrisisEvaluationInput input) {
        // Elige la primera intervencion aplicable y arma el protocolo que luego se persiste y se envia al dispositivo.
        return interventionStrategies.stream()
                .filter(strategy -> strategy.isApplicable(input))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No intervention strategy can prepare a protocol"))
                .execute(input);
    }

    private boolean hasUsableThreshold(ActivationThreshold threshold) {
        return threshold != null
                && Boolean.TRUE.equals(threshold.getActive())
                && (threshold.getBpmMin() != null
                        || threshold.getBpmMax() != null
                        || threshold.getSpo2Min() != null
                        || threshold.getErrorRateMax() != null);
    }

    private boolean exceedsThreshold(CrisisEvaluationInput input, ActivationThreshold threshold) {
        Float currentBpm = input.currentBiometricData().bpm();
        Float currentSpo2 = input.currentBiometricData().spo2();
        Float typingErrorRate = input.typingErrorRate();

        return isBelowMinimum(currentBpm, threshold.getBpmMin())
                || isAboveMaximum(currentBpm, threshold.getBpmMax())
                || isBelowMinimum(currentSpo2, threshold.getSpo2Min())
                || isAboveMaximum(typingErrorRate, threshold.getErrorRateMax());
    }

    private boolean isNearThreshold(CrisisEvaluationInput input, ActivationThreshold threshold) {
        Float currentBpm = input.currentBiometricData().bpm();
        Float currentSpo2 = input.currentBiometricData().spo2();
        Float typingErrorRate = input.typingErrorRate();

        return isNearLowerBound(currentBpm, threshold.getBpmMin(), BPM_THRESHOLD_MARGIN)
                || isNearUpperBound(currentBpm, threshold.getBpmMax(), BPM_THRESHOLD_MARGIN)
                || isNearLowerBound(currentSpo2, threshold.getSpo2Min(), SPO2_THRESHOLD_MARGIN)
                || isNearUpperBound(typingErrorRate, threshold.getErrorRateMax(), ERROR_RATE_THRESHOLD_MARGIN);
    }

    private boolean isTypingErrorInCrisisRange(Float typingErrorRate) {
        return typingErrorRate != null && typingErrorRate >= ERROR_RATE_CRISIS;
    }

    private boolean isTypingErrorInRiskRange(Float typingErrorRate) {
        return typingErrorRate != null && typingErrorRate >= ERROR_RATE_AT_RISK;
    }

    private boolean isBelowMinimum(Float currentValue, Float minimumValue) {
        return currentValue != null && minimumValue != null && currentValue < minimumValue;
    }

    private boolean isAboveMaximum(Float currentValue, Float maximumValue) {
        return currentValue != null && maximumValue != null && currentValue > maximumValue;
    }

    private boolean isNearLowerBound(Float currentValue, Float minimumValue, float margin) {
        return currentValue != null
                && minimumValue != null
                && currentValue >= minimumValue
                && currentValue <= minimumValue + margin;
    }

    private boolean isNearUpperBound(Float currentValue, Float maximumValue, float margin) {
        return currentValue != null
                && maximumValue != null
                && currentValue <= maximumValue
                && currentValue >= maximumValue - margin;
    }

    private void validateThresholdRange(ActivationThreshold threshold) {
        if (threshold.getBpmMin() != null
                && threshold.getBpmMax() != null
                && threshold.getBpmMin() > threshold.getBpmMax()) {
            throw new IllegalArgumentException("Threshold BPM range is invalid");
        }
    }

    private void notifyObservers(PatientStateUpdate update) {
        observers.forEach(observer -> observer.onPatientStateChanged(update));
    }

    private PatientStateUpdate buildPatientStateUpdate(CrisisEvaluationInput input, CrisisMediationResult result) {
        return PatientStateUpdate.monitoring(
                input.patientId(),
                result.emotionalState(),
                result.crisisDetected(),
                result.interventionPrepared(),
                input.currentBiometricData().timestamp()
        );
    }

    private StateEnum maxSeverity(StateEnum left, StateEnum right) {
        if (right == null) {
            return left;
        }
        return severityOf(right) > severityOf(left) ? right : left;
    }

    private int severityOf(StateEnum state) {
        return switch (state) {
            case NORMAL -> 0;
            case RISK_ELEVATED -> 1;
            case ACTIVE_CRISIS -> 2;
        };
    }

    public record CrisisEvaluationInput(
            Long patientId,
            BiometricData currentBiometricData,
            BaseLine baseLine,
            ActivationThreshold activationThreshold,
            Float typingErrorRate,
            Float dwellTime,
            Float flightTime,
            Integer typingErrorCount,
            StateEnum analysisStateHint) {

        public CrisisEvaluationInput(Long patientId,
                                    BiometricData currentBiometricData,
                                    BaseLine baseLine,
                                    ActivationThreshold activationThreshold,
                                    Float typingErrorRate) {
            this(patientId, currentBiometricData, baseLine, activationThreshold, typingErrorRate, null, null, null, null);
        }

        public CrisisEvaluationInput(Long patientId,
                                    BiometricData currentBiometricData,
                                    BaseLine baseLine,
                                    ActivationThreshold activationThreshold,
                                    Float typingErrorRate,
                                    Float dwellTime,
                                    Float flightTime,
                                    Integer typingErrorCount) {
            this(patientId, currentBiometricData, baseLine, activationThreshold, typingErrorRate, dwellTime, flightTime, typingErrorCount, null);
        }

        public CrisisEvaluationInput {
            if (patientId == null || patientId <= 0) {
                throw new IllegalArgumentException("Patient reference must be a positive identifier");
            }
            if (currentBiometricData == null) {
                throw new IllegalArgumentException("Current biometric data is required");
            }
            if (typingErrorRate != null && (!Float.isFinite(typingErrorRate) || typingErrorRate < 0.0f)) {
                throw new IllegalArgumentException("Typing error rate must be a finite non-negative value");
            }
            if (dwellTime != null && (!Float.isFinite(dwellTime) || dwellTime < 0.0f)) {
                throw new IllegalArgumentException("Typing dwell time must be a finite non-negative value");
            }
            if (flightTime != null && (!Float.isFinite(flightTime) || flightTime < 0.0f)) {
                throw new IllegalArgumentException("Typing flight time must be a finite non-negative value");
            }
            if (typingErrorCount != null && typingErrorCount < 0) {
                throw new IllegalArgumentException("Typing error count must be non-negative");
            }
            if (baseLine != null
                    && baseLine.getPatientId() != null
                    && !patientId.equals(baseLine.getPatientId())) {
                throw new IllegalArgumentException("Baseline patient must match the mediation patient");
            }
            if (!hasReadyBaseline(baseLine) && !hasUsableThreshold(activationThreshold)) {
                throw new IllegalArgumentException(
                        "A ready baseline or an active threshold is required for crisis mediation");
            }
        }

        private static boolean hasReadyBaseline(BaseLine baseLine) {
            return baseLine != null && baseLine.isReady();
        }

        private static boolean hasUsableThreshold(ActivationThreshold threshold) {
            return threshold != null
                    && Boolean.TRUE.equals(threshold.getActive())
                    && (threshold.getBpmMin() != null
                            || threshold.getBpmMax() != null
                            || threshold.getSpo2Min() != null
                            || threshold.getErrorRateMax() != null);
        }
    }

    public record CrisisMediationResult(
            EmotionalState emotionalState,
            CrisisEvent crisisEvent,
            InterventionProtocol interventionProtocol,
            boolean crisisDetected,
            boolean interventionPrepared) {

        public CrisisMediationResult {
            if (emotionalState == null) {
                throw new IllegalArgumentException("Emotional state is required");
            }
            if (crisisDetected && crisisEvent == null) {
                throw new IllegalArgumentException("Crisis event is required when a crisis is detected");
            }
            if (interventionPrepared && interventionProtocol == null) {
                throw new IllegalArgumentException(
                        "Intervention protocol is required when an intervention is prepared");
            }
            if (interventionPrepared && !crisisDetected) {
                throw new IllegalArgumentException("Intervention cannot be prepared without a detected crisis");
            }
        }

        public static CrisisMediationResult withoutCrisis(EmotionalState emotionalState) {
            return new CrisisMediationResult(emotionalState, null, null, false, false);
        }

        public static CrisisMediationResult crisisDetected(EmotionalState emotionalState,
                                                        CrisisEvent crisisEvent,
                                                        InterventionProtocol interventionProtocol) {
            return new CrisisMediationResult(emotionalState, crisisEvent, interventionProtocol, true, true);
        }
    }
}
