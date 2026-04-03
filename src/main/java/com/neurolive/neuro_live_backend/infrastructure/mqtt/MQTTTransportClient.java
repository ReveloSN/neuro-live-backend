package com.neurolive.neuro_live_backend.infrastructure.mqtt;

public interface MQTTTransportClient {

    void connect(MQTTProperties properties);

    void disconnect();

    boolean isConnected();

    void publish(String topic, String payload, int qos, boolean retained);

    void subscribe(String topic, int qos, MQTTMessageHandler handler);
}
