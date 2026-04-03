package com.neurolive.neuro_live_backend.infrastructure.mqtt;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
// Mantiene una sola conexion MQTT por instancia del backend
public class MQTTClientManager {

    private static final int MIN_QOS = 0;
    private static final int MAX_QOS = 2;

    private final MQTTProperties mqttProperties;
    private final MQTTTransportClient transportClient;
    private final Set<SubscriptionRegistration> subscriptions = new CopyOnWriteArraySet<>();

    public MQTTClientManager(MQTTProperties mqttProperties, MQTTTransportClient transportClient) {
        this.mqttProperties = requireProperties(mqttProperties);
        this.transportClient = requireTransport(transportClient);
    }

    public synchronized void connect() {
        if (transportClient.isConnected()) {
            return;
        }

        validateConnectionConfiguration();
        transportClient.connect(mqttProperties);
        subscriptions.forEach(subscription ->
                transportClient.subscribe(subscription.topic(), subscription.qos(), subscription.handler()));
    }

    public synchronized void disconnect() {
        if (!transportClient.isConnected()) {
            return;
        }
        transportClient.disconnect();
    }

    public boolean isConnected() {
        return transportClient.isConnected();
    }

    public void publish(String topic, String payload, int qos, boolean retained) {
        String normalizedTopic = validateTopic(topic);
        validatePayload(payload);
        validateQos(qos);
        ensureConnected();

        transportClient.publish(normalizedTopic, payload, qos, retained);
    }

    public void publish(String topic, String payload) {
        publish(topic, payload, mqttProperties.getDefaultQos(), false);
    }

    public synchronized void subscribe(String topic, int qos, MQTTMessageHandler handler) {
        String normalizedTopic = validateTopic(topic);
        validateQos(qos);
        if (handler == null) {
            throw new IllegalArgumentException("MQTT message handler is required");
        }

        SubscriptionRegistration subscription = new SubscriptionRegistration(normalizedTopic, qos, handler);
        if (!subscriptions.add(subscription)) {
            return;
        }

        if (transportClient.isConnected()) {
            transportClient.subscribe(subscription.topic(), subscription.qos(), subscription.handler());
        }
    }

    public void subscribe(String topic, MQTTMessageHandler handler) {
        subscribe(topic, mqttProperties.getDefaultQos(), handler);
    }

    private MQTTProperties requireProperties(MQTTProperties mqttProperties) {
        if (mqttProperties == null) {
            throw new IllegalArgumentException("MQTT properties are required");
        }
        return mqttProperties;
    }

    private MQTTTransportClient requireTransport(MQTTTransportClient transportClient) {
        if (transportClient == null) {
            throw new IllegalArgumentException("MQTT transport client is required");
        }
        return transportClient;
    }

    private void validateConnectionConfiguration() {
        validateTopicLevelValue(mqttProperties.getBrokerUri(), "MQTT broker URI");
        validateTopicLevelValue(mqttProperties.getClientId(), "MQTT client id");
        validateQos(mqttProperties.getDefaultQos());
    }

    private String validateTopic(String topic) {
        return validateTopicLevelValue(topic, "MQTT topic");
    }

    private String validateTopicLevelValue(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private void validatePayload(String payload) {
        if (payload == null) {
            throw new IllegalArgumentException("MQTT payload is required");
        }
    }

    private void validateQos(int qos) {
        if (qos < MIN_QOS || qos > MAX_QOS) {
            throw new IllegalArgumentException("MQTT QoS must be between 0 and 2");
        }
    }

    // Evita duplicar conexiones y conflictos de cliente
    private void ensureConnected() {
        if (!transportClient.isConnected()) {
            throw new IllegalStateException("MQTT client is not connected");
        }
    }

    private record SubscriptionRegistration(String topic, int qos, MQTTMessageHandler handler) {
    }
}
