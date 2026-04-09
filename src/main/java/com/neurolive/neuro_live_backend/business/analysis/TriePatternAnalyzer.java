package com.neurolive.neuro_live_backend.business.analysis;

import com.neurolive.neuro_live_backend.data.enums.StateEnum;
import com.neurolive.neuro_live_backend.domain.analysis.KeystrokeDynamics;
import com.neurolive.neuro_live_backend.infrastructure.config.AnalysisProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Map;

@Component
// Detecta secuencias de tecleo asociadas a escalamiento de riesgo.
public class TriePatternAnalyzer {

    private final AnalysisProperties analysisProperties;
    private final TrieNode root = new TrieNode();

    public TriePatternAnalyzer(AnalysisProperties analysisProperties) {
        this.analysisProperties = analysisProperties;
        registerPattern(List.of("ERROR", "DWELL"), StateEnum.RISK_ELEVATED, "error-dwell");
        registerPattern(List.of("ERROR", "DWELL", "FLIGHT"), StateEnum.ACTIVE_CRISIS, "error-dwell-flight");
        registerPattern(List.of("DWELL", "FLIGHT", "ERROR"), StateEnum.ACTIVE_CRISIS, "slow-typing-crisis");
    }

    public PatternAnalysisResult analyze(Collection<KeystrokeDynamics> keystrokeSamples) {
        if (keystrokeSamples == null || keystrokeSamples.isEmpty()) {
            return PatternAnalysisResult.normal("no-pattern", 0);
        }

        Deque<String> tokens = new ArrayDeque<>();
        keystrokeSamples.stream()
                .sorted(java.util.Comparator.comparing(KeystrokeDynamics::getTimestamp))
                .forEach(sample -> {
                    String token = toToken(sample);
                    if (token != null) {
                        tokens.addLast(token);
                    }
                    while (tokens.size() > analysisProperties.getTrieWindowSize()) {
                        tokens.removeFirst();
                    }
                });

        PatternAnalysisResult bestMatch = PatternAnalysisResult.normal("stable", 0);
        List<String> tokenList = List.copyOf(tokens);
        for (int start = 0; start < tokenList.size(); start++) {
            TrieNode node = root;
            int matchedLength = 0;
            for (int index = start; index < tokenList.size(); index++) {
                node = node.children().get(tokenList.get(index));
                if (node == null) {
                    break;
                }
                matchedLength++;
                if (node.patternName() != null && matchedLength >= bestMatch.matchedLength()) {
                    bestMatch = new PatternAnalysisResult(node.state(), node.patternName(), matchedLength);
                }
            }
        }

        return bestMatch;
    }

    private void registerPattern(List<String> tokens, StateEnum state, String patternName) {
        TrieNode node = root;
        for (String token : tokens) {
            node = node.children().computeIfAbsent(token, ignored -> new TrieNode());
        }
        node.state(state);
        node.patternName(patternName);
    }

    private String toToken(KeystrokeDynamics sample) {
        if (sample.getErrorRate() != null && sample.getErrorRate() >= 0.22f) {
            return "ERROR";
        }
        if (sample.getDwellTime() >= 200.0f) {
            return "DWELL";
        }
        if (sample.getFlightTime() >= 250.0f) {
            return "FLIGHT";
        }
        return null;
    }

    public record PatternAnalysisResult(StateEnum inferredState, String matchedPattern, int matchedLength) {

        public static PatternAnalysisResult normal(String matchedPattern, int matchedLength) {
            return new PatternAnalysisResult(StateEnum.NORMAL, matchedPattern, matchedLength);
        }
    }

    private static final class TrieNode {

        private final Map<String, TrieNode> children = new java.util.HashMap<>();
        private StateEnum state;
        private String patternName;

        private Map<String, TrieNode> children() {
            return children;
        }

        private void state(StateEnum state) {
            this.state = state;
        }

        private StateEnum state() {
            return state == null ? StateEnum.NORMAL : state;
        }

        private void patternName(String patternName) {
            this.patternName = patternName;
        }

        private String patternName() {
            return patternName;
        }
    }
}
