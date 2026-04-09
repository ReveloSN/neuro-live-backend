package com.neurolive.neuro_live_backend.business.patterns;

import com.neurolive.neuro_live_backend.data.enums.TypeEnum;
import com.neurolive.neuro_live_backend.domain.biometric.ActivationThreshold;
import com.neurolive.neuro_live_backend.domain.biometric.BaseLine;
import com.neurolive.neuro_live_backend.domain.crisis.InterventionProtocol;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
// Genera un patron de respiracion guiada para acompañar la regulacion fisiologica durante la crisis.
public class BreathingIntervention implements InterventionStrategy {

    private static final float SPO2_CRISIS_DROP = 5.0f;
    private final Integer rhythm;
    private final Integer cycles;

    public BreathingIntervention() {
        this(4, 6);
    }

    BreathingIntervention(Integer rhythm, Integer cycles) {
        this.rhythm = rhythm;
        this.cycles = cycles;
    }

    @Override
    public TypeEnum getType() {
        return TypeEnum.BREATHING;
    }

    @Override
    public boolean isApplicable(CrisisMediator.CrisisEvaluationInput input) {
        ActivationThreshold activationThreshold = input.activationThreshold();

        if (activationThreshold != null
                && Boolean.TRUE.equals(activationThreshold.getActive())
                && activationThreshold.getSpo2Min() != null) {
            return input.currentBiometricData().spo2() < activationThreshold.getSpo2Min();
        }

        BaseLine baseLine = input.baseLine();
        return baseLine != null
                && baseLine.isReady()
                && (baseLine.getAvgSpo2() - input.currentBiometricData().spo2()) >= SPO2_CRISIS_DROP;
    }

    @Override
    public InterventionProtocol execute(CrisisMediator.CrisisEvaluationInput input) {
        return InterventionProtocol.builder(getType())
                .breathingPattern(rhythm, cycles)
                .build();
    }
}
