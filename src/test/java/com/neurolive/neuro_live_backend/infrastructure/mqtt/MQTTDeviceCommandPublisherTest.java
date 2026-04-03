package com.neurolive.neuro_live_backend.infrastructure.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neurolive.neuro_live_backend.domain.biometric.DeviceCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MQTTDeviceCommandPublisherTest {

    @Mock
    private MQTTClientManager mqttClientManager;

    @Test
    void shouldPublishCommandPayloadThroughTheMqttClientManager() {
        MQTTProperties mqttProperties = new MQTTProperties();
        mqttProperties.setDefaultQos(1);
        mqttProperties.setCommandTopicTemplate("neurolive/devices/%s/commands");

        MQTTDeviceCommandPublisher publisher = new MQTTDeviceCommandPublisher(
                mqttClientManager,
                mqttProperties,
                new ObjectMapper().findAndRegisterModules()
        );

        DeviceCommand command = new DeviceCommand(
                18L,
                "AA:BB:CC:DD:EE:18",
                91L,
                "GUIDED_BREATHING",
                LocalDateTime.of(2026, 4, 2, 10, 40),
                "{\"fallback\":true}"
        );

        publisher.publish(command);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(mqttClientManager).connect();
        verify(mqttClientManager).publish(
                org.mockito.ArgumentMatchers.eq("neurolive/devices/AA:BB:CC:DD:EE:18/commands"),
                payloadCaptor.capture(),
                org.mockito.ArgumentMatchers.eq(1),
                org.mockito.ArgumentMatchers.eq(false)
        );
        assertTrue(payloadCaptor.getValue().contains("\"command\":\"GUIDED_BREATHING\""));
    }
}
