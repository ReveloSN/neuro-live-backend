package com.neurolive.neuro_live_backend.infrastructure.mqtt;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
// Reemplaza el transporte simulado por un cliente MQTT real
public class PahoMQTTTransportClient implements MQTTTransportClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(PahoMQTTTransportClient.class);

    private final PahoClientFactory clientFactory;

    private volatile MqttAsyncClient client;

    public PahoMQTTTransportClient() {
        this(new DefaultPahoClientFactory());
    }

    PahoMQTTTransportClient(PahoClientFactory clientFactory) {
        if (clientFactory == null) {
            throw new IllegalArgumentException("Paho client factory is required");
        }
        this.clientFactory = clientFactory;
    }

    @Override
    // Mantiene una sola conexion administrada por el backend
    public synchronized void connect(MQTTProperties properties) {
        if (isConnected()) {
            return;
        }

        validateConnectionProperties(properties);

        try {
            MqttAsyncClient mqttClient = getOrCreateClient(properties);
            IMqttToken connectToken = mqttClient.connect(buildConnectOptions(properties));
            connectToken.waitForCompletion();
        } catch (MqttException exception) {
            throw new IllegalStateException(
                    "Failed to connect to the MQTT broker " + properties.getBrokerUri(),
                    exception
            );
        }
    }

    @Override
    public synchronized void disconnect() {
        if (client == null) {
            return;
        }

        try {
            if (client.isConnected()) {
                IMqttToken disconnectToken = client.disconnect();
                disconnectToken.waitForCompletion();
            }
            client.close();
        } catch (MqttException exception) {
            throw new IllegalStateException("Failed to disconnect the MQTT client cleanly", exception);
        } finally {
            client = null;
        }
    }

    @Override
    public boolean isConnected() {
        return client != null && client.isConnected();
    }

    @Override
    // Publica comandos hacia el broker con QoS configurable
    public void publish(String topic, String payload, int qos, boolean retained) {
        MqttAsyncClient mqttClient = requireConnectedClient();
        MqttMessage mqttMessage = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
        mqttMessage.setQos(qos);
        mqttMessage.setRetained(retained);

        try {
            IMqttDeliveryToken deliveryToken = mqttClient.publish(topic, mqttMessage);
            deliveryToken.waitForCompletion();
        } catch (MqttException exception) {
            throw new IllegalStateException("Failed to publish MQTT message to topic " + topic, exception);
        }
    }

    @Override
    // Suscribe el backend al topico de telemetria
    public void subscribe(String topic, int qos, MQTTMessageHandler handler) {
        MqttAsyncClient mqttClient = requireConnectedClient();
        IMqttMessageListener listener = (receivedTopic, message) ->
                handler.handle(receivedTopic, new String(message.getPayload(), StandardCharsets.UTF_8));

        try {
            IMqttToken subscriptionToken = mqttClient.subscribe(topic, qos, listener);
            subscriptionToken.waitForCompletion();
        } catch (MqttException exception) {
            throw new IllegalStateException("Failed to subscribe to MQTT topic " + topic, exception);
        }
    }

    private MqttAsyncClient getOrCreateClient(MQTTProperties properties) throws MqttException {
        if (client != null) {
            return client;
        }

        MqttAsyncClient mqttClient = clientFactory.create(
                properties.getBrokerUri(),
                properties.getClientId(),
                new MemoryPersistence()
        );
        mqttClient.setCallback(new LoggingMqttCallback());
        client = mqttClient;
        return mqttClient;
    }

    private MqttAsyncClient requireConnectedClient() {
        if (!isConnected()) {
            throw new IllegalStateException("MQTT client is not connected");
        }
        return client;
    }

    private void validateConnectionProperties(MQTTProperties properties) {
        if (properties == null) {
            throw new IllegalArgumentException("MQTT properties are required");
        }
        if (properties.getBrokerUri() == null || properties.getBrokerUri().isBlank()) {
            throw new IllegalArgumentException("MQTT broker URI is required");
        }
        if (properties.getClientId() == null || properties.getClientId().isBlank()) {
            throw new IllegalArgumentException("MQTT client id is required");
        }
        if (properties.isSecure() && !properties.getBrokerUri().startsWith("ssl://")) {
            throw new IllegalArgumentException("Secure MQTT connections require an ssl:// broker URI");
        }
    }

    private MqttConnectOptions buildConnectOptions(MQTTProperties properties) {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(new String[]{properties.getBrokerUri()});
        options.setCleanSession(true);
        options.setAutomaticReconnect(false);

        if (properties.getUsername() != null && !properties.getUsername().isBlank()) {
            options.setUserName(properties.getUsername().trim());
        }
        if (properties.getPassword() != null && !properties.getPassword().isBlank()) {
            options.setPassword(properties.getPassword().toCharArray());
        }

        return options;
    }

    private static final class LoggingMqttCallback implements MqttCallbackExtended {

        @Override
        public void connectComplete(boolean reconnect, String serverUri) {
            LOGGER.info("Connected to MQTT broker {}", serverUri);
        }

        @Override
        public void connectionLost(Throwable cause) {
            LOGGER.warn("MQTT connection was lost", cause);
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) {
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
        }
    }

    interface PahoClientFactory {

        MqttAsyncClient create(String brokerUri, String clientId, MqttClientPersistence persistence) throws MqttException;
    }

    private static final class DefaultPahoClientFactory implements PahoClientFactory {

        @Override
        public MqttAsyncClient create(String brokerUri, String clientId, MqttClientPersistence persistence)
                throws MqttException {
            return new MqttAsyncClient(brokerUri, clientId, persistence);
        }
    }
}
