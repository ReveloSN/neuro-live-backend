package com.neurolive.neuro_live_backend.business.service;

import com.neurolive.neuro_live_backend.business.analysis.CrisisFeatureVector;
import com.neurolive.neuro_live_backend.business.analysis.KDTreeClassifier;
import com.neurolive.neuro_live_backend.business.analysis.TriePatternAnalyzer;
import com.neurolive.neuro_live_backend.data.enums.StateEnum;
import com.neurolive.neuro_live_backend.domain.analysis.KeystrokeDynamics;
import com.neurolive.neuro_live_backend.domain.biometric.BaseLine;
import com.neurolive.neuro_live_backend.domain.biometric.BiometricData;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class RiskAssessmentService {

    private static final int DEFAULT_PATTERN_WINDOW = 8;

    private final KeystrokeDynamicsService keystrokeDynamicsService;
    private final TriePatternAnalyzer triePatternAnalyzer;
    private final KDTreeClassifier kdTreeClassifier;

    public RiskAssessmentService(KeystrokeDynamicsService keystrokeDynamicsService,
                                 TriePatternAnalyzer triePatternAnalyzer,
                                 KDTreeClassifier kdTreeClassifier) {
        this.keystrokeDynamicsService = keystrokeDynamicsService;
        this.triePatternAnalyzer = triePatternAnalyzer;
        this.kdTreeClassifier = kdTreeClassifier;
    }

    public AssessmentSnapshot assess(Long userId, BiometricData currentBiometricData, BaseLine baseLine) {
        List<KeystrokeDynamics> recentKeystrokes = keystrokeDynamicsService.findRecentForUser(userId, DEFAULT_PATTERN_WINDOW);
        KeystrokeDynamics latestKeystroke = recentKeystrokes.isEmpty() ? null : recentKeystrokes.getFirst();
        TriePatternAnalyzer.PatternAnalysisResult patternAnalysisResult = triePatternAnalyzer.analyze(recentKeystrokes);

        float bpmDelta = baseLine != null && baseLine.isReady()
                ? currentBiometricData.bpm() - baseLine.getAvgBpm()
                : 0.0f;
        float spo2Drop = baseLine != null && baseLine.isReady()
                ? baseLine.getAvgSpo2() - currentBiometricData.spo2()
                : 0.0f;
        float errorRate = latestKeystroke != null && latestKeystroke.getErrorRate() != null
                ? latestKeystroke.getErrorRate()
                : 0.0f;
        float dwellTime = latestKeystroke == null ? 0.0f : latestKeystroke.getDwellTime();
        float flightTime = latestKeystroke == null ? 0.0f : latestKeystroke.getFlightTime();

        KDTreeClassifier.ClassificationResult classificationResult = kdTreeClassifier.classify(
                new CrisisFeatureVector(bpmDelta, spo2Drop, errorRate, dwellTime, flightTime)
        );

        StateEnum latestState = latestKeystroke == null
                ? StateEnum.NORMAL
                : latestKeystroke.analyzePattern().state();
        StateEnum inferredState = maxSeverity(
                maxSeverity(patternAnalysisResult.inferredState(), classificationResult.state()),
                latestState
        );

        return new AssessmentSnapshot(
                latestKeystroke == null ? null : latestKeystroke.getErrorRate(),
                latestKeystroke == null ? null : latestKeystroke.getDwellTime(),
                latestKeystroke == null ? null : latestKeystroke.getFlightTime(),
                latestKeystroke == null ? null : latestKeystroke.getErrorCount(),
                inferredState,
                patternAnalysisResult.matchedPattern(),
                classificationResult.matchedProfile()
        );
    }

    private StateEnum maxSeverity(StateEnum left, StateEnum right) {
        return severityOf(right) > severityOf(left) ? right : left;
    }

    private int severityOf(StateEnum state) {
        return switch (state) {
            case NORMAL -> 0;
            case RISK_ELEVATED -> 1;
            case ACTIVE_CRISIS -> 2;
        };
    }

    public record AssessmentSnapshot(
            Float errorRate,
            Float dwellTime,
            Float flightTime,
            Integer errorCount,
            StateEnum inferredState,
            String matchedPattern,
            String matchedProfile
    ) {
    }
}
