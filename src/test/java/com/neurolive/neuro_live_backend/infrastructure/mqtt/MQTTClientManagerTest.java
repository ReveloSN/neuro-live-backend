package com.neurolive.neuro_live_backend.infrastructure.mqtt;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MQTTClientManagerTest {

    @Test
    void shouldExposeOneManagedConnectionLifecyclePath() {
        RecordingTransportClient transportClient = new RecordingTransportClient();
        MQTTClientManager mqttClientManager = new MQTTClientManager(buildProperties(), transportClient);

        mqttClientManager.connect();
        mqttClientManager.disconnect();

        assertEquals(1, transportClient.connectCalls);
        assertEquals(1, transportClient.disconnectCalls);
        assertFalse(mqttClientManager.isConnected());
    }

    @Test
    void shouldRejectInvalidPublishInputsSafely() {
        MQTTClientManager mqttClientManager = new MQTTClientManager(buildProperties(), new RecordingTransportClient());

        assertThrows(IllegalArgumentException.class, () -> mqttClientManager.publish(" ", "{}", 1, false));
        assertThrows(IllegalArgumentException.class, () -> mqttClientManager.publish("neurolive/alerts", null, 1, false));
        assertThrows(IllegalArgumentException.class, () -> mqttClientManager.publish("neurolive/alerts", "{}", 3, false));
    }

    @Test
    void shouldReportConnectionStateConsistently() {
        MQTTClientManager mqttClientManager = new MQTTClientManager(buildProperties(), new RecordingTransportClient());

        assertFalse(mqttClientManager.isConnected());

        mqttClientManager.connect();
        assertTrue(mqttClientManager.isConnected());

        mqttClientManager.disconnect();
        assertFalse(mqttClientManager.isConnected());
    }

    @Test
    void shouldDelegatePublishSafelyWhenConnected() {
        RecordingTransportClient transportClient = new RecordingTransportClient();
        MQTTClientManager mqttClientManager = new MQTTClientManager(buildProperties(), transportClient);
        mqttClientManager.connect();

        mqttClientManager.publish("neurolive/alerts", "{\"state\":\"CRISIS\"}", 1, false);

        assertEquals("neurolive/alerts", transportClient.lastPublishedTopic);
        assertEquals("{\"state\":\"CRISIS\"}", transportClient.lastPublishedPayload);
        assertEquals(1, transportClient.lastPublishedQos);
        assertFalse(transportClient.lastPublishedRetained);
    }

    @Test
    void shouldAvoidDuplicateInitializationPathsIfSingletonBehaviorIsRelevant() {
        RecordingTransportClient transportClient = new RecordingTransportClient();
        MQTTClientManager mqttClientManager = new MQTTClientManager(buildProperties(), transportClient);

        mqttClientManager.connect();
        mqttClientManager.connect();

        assertEquals(1, transportClient.connectCalls);
    }

    @Test
    void shouldSupportNoOpFallbackTransportClearly() {
        MQTTClientManager mqttClientManager = new MQTTClientManager(buildProperties(), new NoOpMQTTTransportClient());

        mqttClientManager.subscribe("neurolive/telemetry", 1, (topic, payload) -> {
        });
        mqttClientManager.connect();

        assertTrue(mqttClientManager.isConnected());
        assertDoesNotThrow(() -> mqttClientManager.publish("neurolive/alerts", "{\"ok\":true}", 1, false));
    }

    private MQTTProperties buildProperties() {
        MQTTProperties mqttProperties = new MQTTProperties();
        mqttProperties.setBrokerUri("ssl://broker.neurolive.test:8883");
        mqttProperties.setClientId("neuro-live-backend-test");
        mqttProperties.setDefaultQos(1);
        mqttProperties.setSecure(true);
        return mqttProperties;
    }

    private static final class RecordingTransportClient implements MQTTTransportClient {

        private final List<String> subscribedTopics = new ArrayList<>();
        private boolean connected;
        private int connectCalls;
        private int disconnectCalls;
        private String lastPublishedTopic;
        private String lastPublishedPayload;
        private int lastPublishedQos;
        private boolean lastPublishedRetained;

        @Override
        public void connect(MQTTProperties properties) {
            connected = true;
            connectCalls++;
        }

        @Override
        public void disconnect() {
            connected = false;
            disconnectCalls++;
        }

        @Override
        public boolean isConnected() {
            return connected;
        }

        @Override
        public void publish(String topic, String payload, int qos, boolean retained) {
            lastPublishedTopic = topic;
            lastPublishedPayload = payload;
            lastPublishedQos = qos;
            lastPublishedRetained = retained;
        }

        @Override
        public void subscribe(String topic, int qos, MQTTMessageHandler handler) {
            subscribedTopics.add(topic + ":" + qos);
        }
    }
}
