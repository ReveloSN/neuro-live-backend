package com.neurolive.neuro_live_backend.infrastructure.mqtt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neurolive.neuro_live_backend.business.service.TelemetryIngestionService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TelemetryMQTTSubscriber implements MQTTMessageHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(TelemetryMQTTSubscriber.class);

    private final MQTTClientManager mqttClientManager;
    private final MQTTProperties mqttProperties;
    private final TelemetryIngestionService telemetryIngestionService;
    private final ObjectMapper objectMapper;

    public TelemetryMQTTSubscriber(MQTTClientManager mqttClientManager,
                                   MQTTProperties mqttProperties,
                                   TelemetryIngestionService telemetryIngestionService,
                                   ObjectMapper objectMapper) {
        this.mqttClientManager = mqttClientManager;
        this.mqttProperties = mqttProperties;
        this.telemetryIngestionService = telemetryIngestionService;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void registerSubscription() {
        if (!mqttProperties.isAutoStartup()) {
            return;
        }
        mqttClientManager.connect();
        mqttClientManager.subscribe(mqttProperties.getTelemetryTopic(), mqttProperties.getTelemetryQos(), this);
    }

    @Override
    // Reenvia el resultado al flujo de observadores
    public void handle(String topic, String payload) {
        try {
            LOGGER.debug("Received telemetry MQTT message topic={} payload={}", topic, payload);
            TelemetryPayload telemetryPayload = objectMapper.readValue(payload, TelemetryPayload.class);
            LOGGER.debug(
                    "Deserialized telemetry payload patientId={} deviceMac={} bpm={} spo2={} observedAt={}",
                    telemetryPayload.patientId(),
                    telemetryPayload.deviceMac(),
                    telemetryPayload.bpm(),
                    telemetryPayload.spo2(),
                    telemetryPayload.observedAt()
            );
            telemetryIngestionService.ingest(telemetryPayload);
        } catch (JsonProcessingException exception) {
            LOGGER.warn(
                    "Rejected telemetry payload from topic {} due to parsing error: {}",
                    topic,
                    exception.getOriginalMessage()
            );
        } catch (IllegalArgumentException exception) {
            LOGGER.warn(
                    "Rejected telemetry payload from topic {} due to validation error: {}",
                    topic,
                    exception.getMessage()
            );
        } catch (RuntimeException exception) {
            LOGGER.warn("Telemetry ingestion failed for topic {}: {}", topic, exception.getMessage(), exception);
        }
    }
}
