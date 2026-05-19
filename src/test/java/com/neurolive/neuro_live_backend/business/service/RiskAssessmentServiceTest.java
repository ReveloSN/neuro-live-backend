package com.neurolive.neuro_live_backend.business.service;

import com.neurolive.neuro_live_backend.business.analysis.CrisisFeatureVector;
import com.neurolive.neuro_live_backend.business.analysis.KDTreeClassifier;
import com.neurolive.neuro_live_backend.business.analysis.TriePatternAnalyzer;
import com.neurolive.neuro_live_backend.data.enums.StateEnum;
import com.neurolive.neuro_live_backend.domain.analysis.KeystrokeDynamics;
import com.neurolive.neuro_live_backend.domain.biometric.BaseLine;
import com.neurolive.neuro_live_backend.domain.biometric.BiometricData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
// Verifica que RiskAssessmentService combina correctamente señales de KDTree, Trie y dinamica de tecleo.
class RiskAssessmentServiceTest {

    @Mock
    private KeystrokeDynamicsService keystrokeDynamicsService;

    @Mock
    private TriePatternAnalyzer triePatternAnalyzer;

    @Mock
    private KDTreeClassifier kdTreeClassifier;

    private RiskAssessmentService riskAssessmentService;

    @BeforeEach
    void setUp() {
        riskAssessmentService = new RiskAssessmentService(
                keystrokeDynamicsService, triePatternAnalyzer, kdTreeClassifier);
    }

    @Test
    void assess_shouldReturnNormalStateWhenNoKeystrokes() {
        when(keystrokeDynamicsService.findRecentForUser(eq(1L), eq(8))).thenReturn(List.of());
        when(triePatternAnalyzer.analyze(List.of()))
                .thenReturn(new TriePatternAnalyzer.PatternAnalysisResult(StateEnum.NORMAL, null, 0));
        when(kdTreeClassifier.classify(any(CrisisFeatureVector.class)))
                .thenReturn(new KDTreeClassifier.ClassificationResult(StateEnum.NORMAL, 0f, null));

        BiometricData biometricData = new BiometricData(80f, 98f, LocalDateTime.now());
        RiskAssessmentService.AssessmentSnapshot snapshot =
                riskAssessmentService.assess(1L, biometricData, null);

        assertEquals(StateEnum.NORMAL, snapshot.inferredState());
        assertNull(snapshot.errorRate());
    }

    @Test
    void assess_shouldPickMaxSeverityFromKDTreeAndTrie() {
        when(keystrokeDynamicsService.findRecentForUser(eq(2L), eq(8))).thenReturn(List.of());
        when(triePatternAnalyzer.analyze(List.of()))
                .thenReturn(new TriePatternAnalyzer.PatternAnalysisResult(StateEnum.RISK_ELEVATED, "pattern-A", 3));
        when(kdTreeClassifier.classify(any(CrisisFeatureVector.class)))
                .thenReturn(new KDTreeClassifier.ClassificationResult(StateEnum.ACTIVE_CRISIS, 0.1f, "profile-B"));

        BiometricData biometricData = new BiometricData(110f, 97f, LocalDateTime.now());
        RiskAssessmentService.AssessmentSnapshot snapshot =
                riskAssessmentService.assess(2L, biometricData, null);

        assertEquals(StateEnum.ACTIVE_CRISIS, snapshot.inferredState());
        assertEquals("profile-B", snapshot.matchedProfile());
    }

    @Test
    void assess_shouldUseActiveCrisisWhenKeystrokeStateIsHighest() {
        // dwell >= 260 → analyzePattern() returns ACTIVE_CRISIS
        KeystrokeDynamics ks = KeystrokeDynamics.capture(3L, "s1", 270f, 80f, 1, 0.05f, LocalDateTime.now());
        when(keystrokeDynamicsService.findRecentForUser(eq(3L), eq(8))).thenReturn(List.of(ks));
        when(triePatternAnalyzer.analyze(any()))
                .thenReturn(new TriePatternAnalyzer.PatternAnalysisResult(StateEnum.NORMAL, null, 0));
        when(kdTreeClassifier.classify(any(CrisisFeatureVector.class)))
                .thenReturn(new KDTreeClassifier.ClassificationResult(StateEnum.NORMAL, 0f, null));

        BiometricData biometricData = new BiometricData(85f, 98f, LocalDateTime.now());
        RiskAssessmentService.AssessmentSnapshot snapshot =
                riskAssessmentService.assess(3L, biometricData, null);

        assertEquals(StateEnum.ACTIVE_CRISIS, snapshot.inferredState());
    }

    @Test
    void assess_shouldUseBpmDeltaAndSpo2DropFromBaselineForClassification() {
        BaseLine baseLine = new BaseLine(4L);
        LocalDateTime now = LocalDateTime.now();
        baseLine.calculate(List.of(
                new BiometricData(80f, 98f, now),
                new BiometricData(80f, 98f, now.plusSeconds(1)),
                new BiometricData(80f, 98f, now.plusSeconds(2)),
                new BiometricData(80f, 98f, now.plusSeconds(3)),
                new BiometricData(80f, 98f, now.plusSeconds(4)),
                new BiometricData(80f, 98f, now.plusSeconds(5))
        ));

        when(keystrokeDynamicsService.findRecentForUser(eq(4L), eq(8))).thenReturn(List.of());
        when(triePatternAnalyzer.analyze(List.of()))
                .thenReturn(new TriePatternAnalyzer.PatternAnalysisResult(StateEnum.NORMAL, null, 0));
        when(kdTreeClassifier.classify(any(CrisisFeatureVector.class)))
                .thenReturn(new KDTreeClassifier.ClassificationResult(StateEnum.RISK_ELEVATED, 0.3f, "elevated-profile"));

        BiometricData elevated = new BiometricData(105f, 96f, LocalDateTime.now());
        RiskAssessmentService.AssessmentSnapshot snapshot =
                riskAssessmentService.assess(4L, elevated, baseLine);

        assertEquals(StateEnum.RISK_ELEVATED, snapshot.inferredState());
        assertEquals("elevated-profile", snapshot.matchedProfile());
    }
}
