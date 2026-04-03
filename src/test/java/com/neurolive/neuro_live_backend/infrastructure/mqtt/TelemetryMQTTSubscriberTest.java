package com.neurolive.neuro_live_backend.infrastructure.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neurolive.neuro_live_backend.business.service.TelemetryIngestionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TelemetryMQTTSubscriberTest {

    @Mock
    private MQTTClientManager mqttClientManager;

    @Mock
    private TelemetryIngestionService telemetryIngestionService;

    @Test
    void shouldParseAndPersistValidTelemetry() {
        MQTTProperties mqttProperties = new MQTTProperties();
        mqttProperties.setTelemetryTopic("neurolive/telemetry");
        mqttProperties.setTelemetryQos(1);
        TelemetryMQTTSubscriber subscriber = new TelemetryMQTTSubscriber(
                mqttClientManager,
                mqttProperties,
                telemetryIngestionService,
                new ObjectMapper().findAndRegisterModules()
        );

        subscriber.handle(
                "neurolive/telemetry",
                """
                {"patientId":91,"deviceMac":"AA:BB:CC:DD:EE:61","bpm":88.0,"spo2":97.0,"observedAt":"2026-04-02T10:20:00"}
                """
        );

        ArgumentCaptor<TelemetryPayload> payloadCaptor = ArgumentCaptor.forClass(TelemetryPayload.class);
        verify(telemetryIngestionService).ingest(payloadCaptor.capture());
        assertEquals(91L, payloadCaptor.getValue().patientId());
        assertEquals("AA:BB:CC:DD:EE:61", payloadCaptor.getValue().deviceMac());
        assertEquals(88.0f, payloadCaptor.getValue().bpm());
        assertEquals(97.0f, payloadCaptor.getValue().spo2());
        assertEquals(LocalDateTime.of(2026, 4, 2, 10, 20), payloadCaptor.getValue().observedAt());
    }

    @Test
    void shouldRejectInvalidTelemetry() {
        TelemetryMQTTSubscriber subscriber = new TelemetryMQTTSubscriber(
                mqttClientManager,
                new MQTTProperties(),
                telemetryIngestionService,
                new ObjectMapper().findAndRegisterModules()
        );

        subscriber.handle("neurolive/telemetry", "{invalid-json");

        verify(telemetryIngestionService, never()).ingest(any());
    }

    @Test
    void shouldRegisterTelemetrySubscriptionThroughMqttClientManager() {
        MQTTProperties mqttProperties = new MQTTProperties();
        mqttProperties.setTelemetryTopic("neurolive/telemetry");
        mqttProperties.setTelemetryQos(1);
        TelemetryMQTTSubscriber subscriber = new TelemetryMQTTSubscriber(
                mqttClientManager,
                mqttProperties,
                telemetryIngestionService,
                new ObjectMapper().findAndRegisterModules()
        );

        subscriber.registerSubscription();

        verify(mqttClientManager).connect();
        verify(mqttClientManager).subscribe("neurolive/telemetry", 1, subscriber);
    }

    @Test
    void shouldSkipStartupSubscriptionWhenAutoStartupIsDisabled() {
        MQTTProperties mqttProperties = new MQTTProperties();
        mqttProperties.setAutoStartup(false);
        TelemetryMQTTSubscriber subscriber = new TelemetryMQTTSubscriber(
                mqttClientManager,
                mqttProperties,
                telemetryIngestionService,
                new ObjectMapper().findAndRegisterModules()
        );

        subscriber.registerSubscription();

        verifyNoInteractions(mqttClientManager);
    }
}
