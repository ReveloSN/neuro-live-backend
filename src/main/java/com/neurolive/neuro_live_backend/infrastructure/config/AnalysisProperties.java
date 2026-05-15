package com.neurolive.neuro_live_backend.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "analysis")
public class AnalysisProperties {

    private long baselineWindowMinutes = 5;
    private int trieWindowSize = 8;
    private long telemetryWindowMinutes = 10;
    private int kdtreeBootstrapLimit = 500;

    public Duration baselineWindow() {
        return Duration.ofMinutes(baselineWindowMinutes);
    }

    public Duration telemetryWindow() {
        return Duration.ofMinutes(telemetryWindowMinutes);
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

    public long getTelemetryWindowMinutes() {
        return telemetryWindowMinutes;
    }

    public void setTelemetryWindowMinutes(long telemetryWindowMinutes) {
        this.telemetryWindowMinutes = telemetryWindowMinutes;
    }

    public int getKdtreeBootstrapLimit() {
        return kdtreeBootstrapLimit;
    }

    public void setKdtreeBootstrapLimit(int kdtreeBootstrapLimit) {
        this.kdtreeBootstrapLimit = kdtreeBootstrapLimit;
    }
}
