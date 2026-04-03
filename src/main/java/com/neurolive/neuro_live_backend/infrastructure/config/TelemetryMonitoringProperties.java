package com.neurolive.neuro_live_backend.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "telemetry")
public class TelemetryMonitoringProperties {

    private long disconnectTimeoutSeconds = 5L;
    private long disconnectCheckIntervalSeconds = 1L;

    public long getDisconnectTimeoutSeconds() {
        return disconnectTimeoutSeconds;
    }

    public void setDisconnectTimeoutSeconds(long disconnectTimeoutSeconds) {
        this.disconnectTimeoutSeconds = disconnectTimeoutSeconds;
    }

    public long getDisconnectCheckIntervalSeconds() {
        return disconnectCheckIntervalSeconds;
    }

    public void setDisconnectCheckIntervalSeconds(long disconnectCheckIntervalSeconds) {
        this.disconnectCheckIntervalSeconds = disconnectCheckIntervalSeconds;
    }
}
