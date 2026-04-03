package com.neurolive.neuro_live_backend.infrastructure.mqtt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

public class NoOpMQTTTransportClient implements MQTTTransportClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(NoOpMQTTTransportClient.class);

    private final AtomicBoolean connected = new AtomicBoolean(false);

    @Override
    public void connect(MQTTProperties properties) {
        connected.set(true);
        LOGGER.info("MQTT transport placeholder connected for broker {}", properties.getBrokerUri());
    }

    @Override
    public void disconnect() {
        connected.set(false);
        LOGGER.info("MQTT transport placeholder disconnected");
    }

    @Override
    public boolean isConnected() {
        return connected.get();
    }

    @Override
    // Centraliza la publicacion de mensajes hacia el broker
    public void publish(String topic, String payload, int qos, boolean retained) {
        LOGGER.info("MQTT placeholder publish topic={} qos={} retained={} payload={}", topic, qos, retained, payload);
    }

    @Override
    // Prepara el punto de integracion para suscripciones futuras
    public void subscribe(String topic, int qos, MQTTMessageHandler handler) {
        LOGGER.info("MQTT placeholder subscribe topic={} qos={}", topic, qos);
    }
}
