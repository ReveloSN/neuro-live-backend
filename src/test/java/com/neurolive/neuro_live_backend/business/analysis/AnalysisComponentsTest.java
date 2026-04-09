package com.neurolive.neuro_live_backend.business.analysis;

import com.neurolive.neuro_live_backend.data.enums.StateEnum;
import com.neurolive.neuro_live_backend.domain.analysis.KeystrokeDynamics;
import com.neurolive.neuro_live_backend.domain.biometric.BaseLine;
import com.neurolive.neuro_live_backend.domain.biometric.BiometricData;
import com.neurolive.neuro_live_backend.infrastructure.config.AnalysisProperties;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalysisComponentsTest {

    @Test
    void shouldCalculateBaselineWithConfiguredWindow() {
        AnalysisProperties analysisProperties = new AnalysisProperties();
        analysisProperties.setBaselineWindowMinutes(5);
        BaselineCalculator baselineCalculator = new BaselineCalculator(analysisProperties);
        BaseLine baseLine = new BaseLine(500L);
        LocalDateTime start = LocalDateTime.of(2026, 4, 3, 8, 0);

        baselineCalculator.calculate(baseLine, List.of(
                new BiometricData(80.0f, 98.0f, start),
                new BiometricData(82.0f, 98.0f, start.plusMinutes(1)),
                new BiometricData(84.0f, 97.0f, start.plusMinutes(2)),
                new BiometricData(86.0f, 98.0f, start.plusMinutes(3)),
                new BiometricData(88.0f, 99.0f, start.plusMinutes(4)),
                new BiometricData(90.0f, 99.0f, start.plusMinutes(5))
        ));

        assertTrue(baseLine.isReady());
        assertEquals(85.0f, baseLine.getAvgBpm(), 0.0001f);
        assertEquals(98.166664f, baseLine.getAvgSpo2(), 0.0001f);
    }

    @Test
    void shouldClassifyAcuteSignalsAsActiveCrisis() {
        KDTreeClassifier classifier = new KDTreeClassifier();

        KDTreeClassifier.ClassificationResult result = classifier.classify(
                new CrisisFeatureVector(32.0f, 6.0f, 0.30f, 300.0f, 350.0f)
        );

        assertEquals(StateEnum.ACTIVE_CRISIS, result.state());
    }

    @Test
    void shouldDetectEscalatedKeystrokePatternThroughTrie() {
        AnalysisProperties analysisProperties = new AnalysisProperties();
        TriePatternAnalyzer triePatternAnalyzer = new TriePatternAnalyzer(analysisProperties);
        LocalDateTime start = LocalDateTime.of(2026, 4, 3, 9, 0);

        TriePatternAnalyzer.PatternAnalysisResult result = triePatternAnalyzer.analyze(List.of(
                KeystrokeDynamics.capture(40L, "session-a", 140.0f, 160.0f, 3, 0.30f, start),
                KeystrokeDynamics.capture(40L, "session-a", 220.0f, 160.0f, 1, 0.10f, start.plusSeconds(5)),
                KeystrokeDynamics.capture(40L, "session-a", 150.0f, 300.0f, 1, 0.10f, start.plusSeconds(10))
        ));

        assertEquals(StateEnum.ACTIVE_CRISIS, result.inferredState());
        assertEquals("error-dwell-flight", result.matchedPattern());
    }
}
