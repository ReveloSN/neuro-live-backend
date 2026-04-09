package com.neurolive.neuro_live_backend.business.patterns;

import com.neurolive.neuro_live_backend.data.enums.TypeEnum;
import com.neurolive.neuro_live_backend.domain.biometric.ActivationThreshold;
import com.neurolive.neuro_live_backend.domain.crisis.InterventionProtocol;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
// Activa un modo visual mas calmado cuando el patron de tecleo sugiere sobrecarga o desregulacion.
public class UIIntervention implements InterventionStrategy {

    private static final float ERROR_RATE_CRISIS = 0.25f;
    private final String theme;
    private final boolean contrast;

    public UIIntervention() {
        this("calm-focus", true);
    }

    UIIntervention(String theme, boolean contrast) {
        this.theme = theme;
        this.contrast = contrast;
    }

    @Override
    public TypeEnum getType() {
        return TypeEnum.UI;
    }

    @Override
    public boolean isApplicable(CrisisMediator.CrisisEvaluationInput input) {
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
    public InterventionProtocol execute(CrisisMediator.CrisisEvaluationInput input) {
        return InterventionProtocol.builder(getType())
                .uiMode(theme, contrast)
                .build();
    }
}
