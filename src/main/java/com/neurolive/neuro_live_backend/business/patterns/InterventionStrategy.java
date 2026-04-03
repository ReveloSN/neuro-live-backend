package com.neurolive.neuro_live_backend.business.patterns;

import com.neurolive.neuro_live_backend.data.enums.TypeEnum;
import com.neurolive.neuro_live_backend.domain.crisis.InterventionProtocol;

// Define el contrato comun para las intervenciones
public interface InterventionStrategy {

    TypeEnum getType();

    boolean supports(CrisisMediator.CrisisEvaluationInput input);

    InterventionProtocol prepareProtocol(CrisisMediator.CrisisEvaluationInput input);
}
