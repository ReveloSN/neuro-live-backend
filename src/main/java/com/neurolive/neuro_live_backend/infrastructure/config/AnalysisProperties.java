package com.neurolive.neuro_live_backend.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "analysis")
public class AnalysisProperties {

    private long baselineWindowMinutes = 5;
    private int trieWindowSize = 8;

    public Duration baselineWindow() {
        return Duration.ofMinutes(baselineWindowMinutes);
    }

    public long getBaselineWindowMinutes() {
        return baselineWindowMinutes;
    }

    public void setBaselineWindowMinutes(long baselineWindowMinutes) {
        this.baselineWindowMinutes = baselineWindowMinutes;
    }

    public int getTrieWindowSize() {
        return trieWindowSize;
    }

    public void setTrieWindowSize(int trieWindowSize) {
        this.trieWindowSize = trieWindowSize;
    }
}
