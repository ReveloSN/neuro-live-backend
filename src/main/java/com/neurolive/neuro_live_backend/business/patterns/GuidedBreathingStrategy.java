package com.neurolive.neuro_live_backend.business.patterns;

import com.neurolive.neuro_live_backend.data.enums.TypeEnum;
import com.neurolive.neuro_live_backend.domain.biometric.ActivationThreshold;
import com.neurolive.neuro_live_backend.domain.biometric.BaseLine;
import com.neurolive.neuro_live_backend.domain.crisis.InterventionProtocol;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
public class GuidedBreathingStrategy implements InterventionStrategy {

    private static final float SPO2_CRISIS_DROP = 5.0f;

    @Override
    public TypeEnum getType() {
        return TypeEnum.GUIDED_BREATHING;
    }

    @Override
    public boolean supports(CrisisMediator.CrisisEvaluationInput input) {
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
    // Construye el protocolo segun la estrategia elegida
    public InterventionProtocol prepareProtocol(CrisisMediator.CrisisEvaluationInput input) {
        return InterventionProtocol.builder(getType())
                .breathingEnabled()
                .build();
    }
}
