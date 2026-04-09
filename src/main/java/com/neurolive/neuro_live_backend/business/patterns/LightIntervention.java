package com.neurolive.neuro_live_backend.business.patterns;

import com.neurolive.neuro_live_backend.data.enums.TypeEnum;
import com.neurolive.neuro_live_backend.domain.biometric.ActivationThreshold;
import com.neurolive.neuro_live_backend.domain.crisis.InterventionProtocol;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(3)
// Prepara una configuracion de luz calmante para bajar estimulacion visual durante la crisis.
public class LightIntervention implements InterventionStrategy {

    private final String rgbValue;
    private final int brightness;

    public LightIntervention() {
        this("blue", 55);
    }

    LightIntervention(String rgbValue, int brightness) {
        this.rgbValue = rgbValue;
        this.brightness = brightness;
    }

    @Override
    public TypeEnum getType() {
        return TypeEnum.LIGHT;
    }

    @Override
    public boolean isApplicable(CrisisMediator.CrisisEvaluationInput input) {
        ActivationThreshold activationThreshold = input.activationThreshold();

        if (activationThreshold == null || !Boolean.TRUE.equals(activationThreshold.getActive())) {
            return false;
        }

        Float currentBpm = input.currentBiometricData().bpm();
        return (activationThreshold.getBpmMin() != null && currentBpm < activationThreshold.getBpmMin())
                || (activationThreshold.getBpmMax() != null && currentBpm > activationThreshold.getBpmMax());
    }

    @Override
    public InterventionProtocol execute(CrisisMediator.CrisisEvaluationInput input) {
        return InterventionProtocol.builder(getType())
                .light(rgbValue, brightness)
                .build();
    }
}
