package com.neurolive.neuro_live_backend.business.patterns;

import com.neurolive.neuro_live_backend.data.enums.TypeEnum;
import com.neurolive.neuro_live_backend.domain.biometric.ActivationThreshold;
import com.neurolive.neuro_live_backend.domain.crisis.InterventionProtocol;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class UiReductionStrategy implements InterventionStrategy {

    private static final float ERROR_RATE_CRISIS = 0.25f;

    @Override
    public TypeEnum getType() {
        return TypeEnum.UI_REDUCTION;
    }

    @Override
    public boolean supports(CrisisMediator.CrisisEvaluationInput input) {
        Float typingErrorRate = input.typingErrorRate();
        if (typingErrorRate == null) {
            return false;
        }

        ActivationThreshold activationThreshold = input.activationThreshold();
        if (activationThreshold != null
                && Boolean.TRUE.equals(activationThreshold.getActive())
                && activationThreshold.getErrorRateMax() != null) {
            return typingErrorRate > activationThreshold.getErrorRateMax();
        }

        return typingErrorRate >= ERROR_RATE_CRISIS;
    }

    @Override
    public InterventionProtocol prepareProtocol(CrisisMediator.CrisisEvaluationInput input) {
        return InterventionProtocol.builder(getType())
                .uiReductionEnabled()
                .build();
    }
}
