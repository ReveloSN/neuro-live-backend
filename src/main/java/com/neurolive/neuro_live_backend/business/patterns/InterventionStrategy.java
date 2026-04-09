package com.neurolive.neuro_live_backend.business.patterns;

import com.neurolive.neuro_live_backend.data.enums.TypeEnum;
import com.neurolive.neuro_live_backend.domain.crisis.InterventionProtocol;

// Define el contrato comun Strategy. Las implementaciones concretas no se heredan entre si.
public interface InterventionStrategy {

    TypeEnum getType();

    boolean isApplicable(CrisisMediator.CrisisEvaluationInput input);

    InterventionProtocol execute(CrisisMediator.CrisisEvaluationInput input);

    // Permite consultar si una intervencion aplica usando el nombre historico del contrato.
    default boolean supports(CrisisMediator.CrisisEvaluationInput input) {
        return isApplicable(input);
    }

    // Permite construir el protocolo usando el nombre historico del contrato.
    default InterventionProtocol prepareProtocol(CrisisMediator.CrisisEvaluationInput input) {
        return execute(input);
    }
}
