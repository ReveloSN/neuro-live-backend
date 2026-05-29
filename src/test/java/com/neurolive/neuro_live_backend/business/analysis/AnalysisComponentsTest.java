package com.neurolive.neuro_live_backend.business.analysis;

import com.neurolive.neuro_live_backend.data.enums.StateEnum;
import com.neurolive.neuro_live_backend.domain.analysis.KeystrokeDynamics;
import com.neurolive.neuro_live_backend.domain.biometric.BaseLine;
import com.neurolive.neuro_live_backend.domain.biometric.BiometricData;
import com.neurolive.neuro_live_backend.infrastructure.config.AnalysisProperties;
import com.neurolive.neuro_live_backend.repository.CrisisEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AnalysisComponentsTest {

    @Test
    void shouldCalculateBaselineWithConfiguredWindow() {
        AnalysisProperties analysisProperties = new AnalysisProperties();
        analysisProperties.setBaselineWindowMinutes(5);
        BaselineCalculator baselineCalculator = new BaselineCalculator(analysisProperties);
        BaseLine baseLine = new BaseLine(500L);
        Instant start = Instant.parse("2026-04-03T08:00:00Z");

        baselineCalculator.calculate(baseLine, List.of(
                new BiometricData(80.0f, 98.0f, start),
                new BiometricData(82.0f, 98.0f, start.plusSeconds(60)),
                new BiometricData(84.0f, 97.0f, start.plusSeconds(120)),
                new BiometricData(86.0f, 98.0f, start.plusSeconds(180)),
                new BiometricData(88.0f, 99.0f, start.plusSeconds(240)),
                new BiometricData(90.0f, 99.0f, start.plusSeconds(300))
        ));

        assertTrue(baseLine.isReady());
        assertEquals(85.0f, baseLine.getAvgBpm(), 0.0001f);
        assertEquals(98.166664f, baseLine.getAvgSpo2(), 0.0001f);
    }

    @Test
    void shouldClassifyAcuteSignalsAsActiveCrisis() {
        CrisisEventRepository crisisEventRepository = mock(CrisisEventRepository.class);
        when(crisisEventRepository.findAllByStateNotNullOrderByStartedAtDesc(any(Pageable.class)))
                .thenReturn(Page.empty());
        AnalysisProperties localProps = new AnalysisProperties();
        localProps.setKdtreeBootstrapLimit(100);
        KDTreeClassifier classifier = new KDTreeClassifier(crisisEventRepository, localProps);
        classifier.initializeFromDatabase();

        KDTreeClassifier.ClassificationResult result = classifier.classify(
                new CrisisFeatureVector(32.0f, 6.0f, 0.30f, 300.0f, 350.0f)
        );

        assertEquals(StateEnum.ACTIVE_CRISIS, result.state());
    }

    @Test
    void shouldDetectEscalatedKeystrokePatternThroughTrie() {
        AnalysisProperties analysisProperties = new AnalysisProperties();
        TriePatternAnalyzer triePatternAnalyzer = new TriePatternAnalyzer(analysisProperties);
        Instant start = Instant.parse("2026-04-03T09:00:00Z");

        TriePatternAnalyzer.PatternAnalysisResult result = triePatternAnalyzer.analyze(List.of(
                KeystrokeDynamics.capture(40L, "session-a", 140.0f, 160.0f, 3, 0.30f, LocalDateTime.ofInstant(start, ZoneOffset.UTC)),
                KeystrokeDynamics.capture(40L, "session-a", 220.0f, 160.0f, 1, 0.10f, LocalDateTime.ofInstant(start.plusSeconds(5), ZoneOffset.UTC)),
                KeystrokeDynamics.capture(40L, "session-a", 150.0f, 300.0f, 1, 0.10f, LocalDateTime.ofInstant(start.plusSeconds(10), ZoneOffset.UTC))
        ));

        assertEquals(StateEnum.ACTIVE_CRISIS, result.inferredState());
        assertEquals("error-dwell-flight", result.matchedPattern());
    }
}
