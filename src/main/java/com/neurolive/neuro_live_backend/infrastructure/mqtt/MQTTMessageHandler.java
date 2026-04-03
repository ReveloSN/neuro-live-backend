package com.neurolive.neuro_live_backend.infrastructure.mqtt;

public interface MQTTMessageHandler {

    void handle(String topic, String payload);
}
