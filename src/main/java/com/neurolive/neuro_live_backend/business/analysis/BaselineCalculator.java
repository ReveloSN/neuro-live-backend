package com.neurolive.neuro_live_backend.business.analysis;

import com.neurolive.neuro_live_backend.domain.biometric.BaseLine;
import com.neurolive.neuro_live_backend.domain.biometric.BiometricData;
import com.neurolive.neuro_live_backend.infrastructure.config.AnalysisProperties;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
// Encapsula el calculo configurable de la linea base biometrica.
public class BaselineCalculator {

    private final AnalysisProperties analysisProperties;

    public BaselineCalculator(AnalysisProperties analysisProperties) {
        this.analysisProperties = analysisProperties;
    }

    public BaseLine calculate(BaseLine baseLine, Collection<BiometricData> biometricSamples) {
        if (baseLine == null) {
            throw new IllegalArgumentException("Baseline aggregate is required");
        }

        return baseLine.calculate(biometricSamples, analysisProperties.baselineWindow());
    }
}
