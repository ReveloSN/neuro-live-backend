package com.neurolive.neuro_live_backend.infrastructure.mqtt;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mqtt")
public class MQTTProperties {

    private String brokerUri = "ssl://localhost:8883";
    private String clientId = "neuro-live-backend";
    private String username;
    private String password;
    private int defaultQos = 1;
    private boolean secure = true;
    private boolean autoStartup = true;
    private String telemetryTopic = "neurolive/telemetry";
    private int telemetryQos = 1;
    private String commandTopicTemplate = "neurolive/devices/%s/commands";

    public String getBrokerUri() {
        return brokerUri;
    }

    public void setBrokerUri(String brokerUri) {
        this.brokerUri = brokerUri;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getDefaultQos() {
        return defaultQos;
    }

    public void setDefaultQos(int defaultQos) {
        this.defaultQos = defaultQos;
    }

    public boolean isSecure() {
        return secure;
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    public boolean isAutoStartup() {
        return autoStartup;
    }

    public void setAutoStartup(boolean autoStartup) {
        this.autoStartup = autoStartup;
    }

    public String getTelemetryTopic() {
        return telemetryTopic;
    }

    public void setTelemetryTopic(String telemetryTopic) {
        this.telemetryTopic = telemetryTopic;
    }

    public int getTelemetryQos() {
        return telemetryQos;
    }

    public void setTelemetryQos(int telemetryQos) {
        this.telemetryQos = telemetryQos;
    }

    public String getCommandTopicTemplate() {
        return commandTopicTemplate;
    }

    public void setCommandTopicTemplate(String commandTopicTemplate) {
        this.commandTopicTemplate = commandTopicTemplate;
    }
}
