package com.neurolive.neuro_live_backend.business.patterns;

import com.neurolive.neuro_live_backend.data.enums.TypeEnum;
import com.neurolive.neuro_live_backend.domain.crisis.InterventionProtocol;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(4)
// Define el audio de regulacion que se usa como apoyo o fallback cuando otras intervenciones no aplican.
public class AudioIntervention implements InterventionStrategy {

    private final String audioFile;
    private final Integer volume;

    public AudioIntervention() {
        this("calm-default", 35);
    }

    AudioIntervention(String audioFile, Integer volume) {
        this.audioFile = audioFile;
        this.volume = volume;
    }

    @Override
    public TypeEnum getType() {
        return TypeEnum.AUDIO;
    }

    @Override
    public boolean isApplicable(CrisisMediator.CrisisEvaluationInput input) {
        return true;
    }

    @Override
    public InterventionProtocol execute(CrisisMediator.CrisisEvaluationInput input) {
        return InterventionProtocol.builder(getType())
                .audioTrack(audioFile, volume)
                .build();
    }
}
