package com.neurolive.neuro_live_backend.business.patterns;

import com.neurolive.neuro_live_backend.data.enums.TypeEnum;
import com.neurolive.neuro_live_backend.domain.crisis.InterventionProtocol;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(4)
public class AuditoryRegulationStrategy implements InterventionStrategy {

    @Override
    public TypeEnum getType() {
        return TypeEnum.AUDITORY_REGULATION;
    }

    @Override
    public boolean supports(CrisisMediator.CrisisEvaluationInput input) {
        return true;
    }

    @Override
    public InterventionProtocol prepareProtocol(CrisisMediator.CrisisEvaluationInput input) {
        return InterventionProtocol.builder(getType())
                .audioTrack("calm-default")
                .build();
    }
}
