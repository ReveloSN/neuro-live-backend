package com.neurolive.neuro_live_backend.infrastructure.mqtt;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PahoMQTTTransportClientTest {

    @Mock
    private MqttAsyncClient client;

    @Mock
    private IMqttToken connectToken;

    @Mock
    private IMqttToken disconnectToken;

    @Mock
    private IMqttToken subscribeToken;

    @Mock
    private IMqttDeliveryToken deliveryToken;

    @Mock
    private MQTTMessageHandler handler;

    @Test
    void shouldConnectUsingConfiguredBrokerSettings() throws Exception {
        AtomicBoolean connected = new AtomicBoolean(false);
        when(client.isConnected()).thenAnswer(invocation -> connected.get());
        when(client.connect(any(MqttConnectOptions.class))).thenAnswer(invocation -> {
            connected.set(true);
            return connectToken;
        });

        AtomicBrokerSettings capturedSettings = new AtomicBrokerSettings();
        PahoMQTTTransportClient transportClient = new PahoMQTTTransportClient((brokerUri, clientId, persistence) -> {
            capturedSettings.brokerUri = brokerUri;
            capturedSettings.clientId = clientId;
            return client;
        });

        transportClient.connect(buildProperties());

        ArgumentCaptor<MqttConnectOptions> optionsCaptor = ArgumentCaptor.forClass(MqttConnectOptions.class);
        verify(client).connect(optionsCaptor.capture());
        assertEquals("ssl://broker.neurolive.test:8883", capturedSettings.brokerUri);
        assertEquals("neuro-live-backend-test", capturedSettings.clientId);
        assertArrayEquals(new String[]{"ssl://broker.neurolive.test:8883"}, optionsCaptor.getValue().getServerURIs());
        assertEquals("mqtt-user", optionsCaptor.getValue().getUserName());
        assertArrayEquals("mqtt-password".toCharArray(), optionsCaptor.getValue().getPassword());
        assertTrue(transportClient.isConnected());
    }

    @Test
    void shouldPublishValidPayloads() throws Exception {
        AtomicBoolean connected = new AtomicBoolean(false);
        when(client.isConnected()).thenAnswer(invocation -> connected.get());
        when(client.connect(any(MqttConnectOptions.class))).thenAnswer(invocation -> {
            connected.set(true);
            return connectToken;
        });
        when(client.publish(eq("neurolive/alerts"), any(MqttMessage.class))).thenReturn(deliveryToken);

        PahoMQTTTransportClient transportClient = new PahoMQTTTransportClient((brokerUri, clientId, persistence) -> client);
        transportClient.connect(buildProperties());
        transportClient.publish("neurolive/alerts", "{\"state\":\"CRISIS\"}", 1, false);

        ArgumentCaptor<MqttMessage> messageCaptor = ArgumentCaptor.forClass(MqttMessage.class);
        verify(client).publish(eq("neurolive/alerts"), messageCaptor.capture());
        assertEquals("{\"state\":\"CRISIS\"}", new String(messageCaptor.getValue().getPayload(), StandardCharsets.UTF_8));
        assertEquals(1, messageCaptor.getValue().getQos());
        assertFalse(messageCaptor.getValue().isRetained());
    }

    @Test
    void shouldSubscribeAndDispatchMessagesToTheHandler() throws Exception {
        AtomicBoolean connected = new AtomicBoolean(false);
        when(client.isConnected()).thenAnswer(invocation -> connected.get());
        when(client.connect(any(MqttConnectOptions.class))).thenAnswer(invocation -> {
            connected.set(true);
            return connectToken;
        });
        when(client.subscribe(eq("neurolive/telemetry"), eq(1), any(IMqttMessageListener.class))).thenReturn(subscribeToken);

        PahoMQTTTransportClient transportClient = new PahoMQTTTransportClient((brokerUri, clientId, persistence) -> client);
        transportClient.connect(buildProperties());
        transportClient.subscribe("neurolive/telemetry", 1, handler);

        ArgumentCaptor<IMqttMessageListener> listenerCaptor = ArgumentCaptor.forClass(IMqttMessageListener.class);
        verify(client).subscribe(eq("neurolive/telemetry"), eq(1), listenerCaptor.capture());
        listenerCaptor.getValue().messageArrived(
                "neurolive/telemetry",
                new MqttMessage("{\"bpm\":88}".getBytes(StandardCharsets.UTF_8))
        );

        verify(handler).handle("neurolive/telemetry", "{\"bpm\":88}");
    }

    @Test
    void shouldReportConnectionStateConsistently() throws Exception {
        AtomicBoolean connected = new AtomicBoolean(false);
        when(client.isConnected()).thenAnswer(invocation -> connected.get());
        when(client.connect(any(MqttConnectOptions.class))).thenAnswer(invocation -> {
            connected.set(true);
            return connectToken;
        });
        when(client.disconnect()).thenAnswer(invocation -> {
            connected.set(false);
            return disconnectToken;
        });

        PahoMQTTTransportClient transportClient = new PahoMQTTTransportClient((brokerUri, clientId, persistence) -> client);

        assertFalse(transportClient.isConnected());

        transportClient.connect(buildProperties());
        assertTrue(transportClient.isConnected());

        transportClient.disconnect();
        assertFalse(transportClient.isConnected());
        verify(client).close();
    }

    @Test
    void shouldRejectInsecureBrokerUriWhenSecureTransportIsRequired() {
        MQTTProperties properties = buildProperties();
        properties.setBrokerUri("tcp://broker.neurolive.test:1883");
        properties.setSecure(true);

        PahoMQTTTransportClient transportClient = new PahoMQTTTransportClient((brokerUri, clientId, persistence) -> client);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> transportClient.connect(properties)
        );

        assertEquals("Secure MQTT connections require an ssl:// broker URI", exception.getMessage());
    }

    private MQTTProperties buildProperties() {
        MQTTProperties properties = new MQTTProperties();
        properties.setBrokerUri("ssl://broker.neurolive.test:8883");
        properties.setClientId("neuro-live-backend-test");
        properties.setUsername("mqtt-user");
        properties.setPassword("mqtt-password");
        properties.setDefaultQos(1);
        properties.setSecure(true);
        return properties;
    }

    private static final class AtomicBrokerSettings {

        private String brokerUri;
        private String clientId;
    }
}
