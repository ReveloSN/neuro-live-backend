package com.neurolive.neuro_live_backend.business.patterns;

import com.neurolive.neuro_live_backend.data.enums.TypeEnum;
import com.neurolive.neuro_live_backend.domain.biometric.ActivationThreshold;
import com.neurolive.neuro_live_backend.domain.crisis.InterventionProtocol;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(3)
public class LightingInterventionStrategy implements InterventionStrategy {

    @Override
    public TypeEnum getType() {
        return TypeEnum.LIGHTING_CONTROL;
    }

    @Override
    public boolean supports(CrisisMediator.CrisisEvaluationInput input) {
        ActivationThreshold activationThreshold = input.activationThreshold();

        if (activationThreshold == null || !Boolean.TRUE.equals(activationThreshold.getActive())) {
            return false;
        }

        Float currentBpm = input.currentBiometricData().bpm();
        return (activationThreshold.getBpmMin() != null && currentBpm < activationThreshold.getBpmMin())
                || (activationThreshold.getBpmMax() != null && currentBpm > activationThreshold.getBpmMax());
    }

    @Override
    public InterventionProtocol prepareProtocol(CrisisMediator.CrisisEvaluationInput input) {
        return InterventionProtocol.builder(getType())
                .light("blue", 55)
                .build();
    }
}
