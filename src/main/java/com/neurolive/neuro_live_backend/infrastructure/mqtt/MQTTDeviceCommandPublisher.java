package com.neurolive.neuro_live_backend.infrastructure.mqtt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neurolive.neuro_live_backend.domain.biometric.DeviceCommand;
import org.springframework.stereotype.Component;

@Component
public class MQTTDeviceCommandPublisher implements DeviceCommandPublisher {

    private final MQTTClientManager mqttClientManager;
    private final MQTTProperties mqttProperties;
    private final ObjectMapper objectMapper;

    public MQTTDeviceCommandPublisher(MQTTClientManager mqttClientManager,
                                      MQTTProperties mqttProperties,
                                      ObjectMapper objectMapper) {
        this.mqttClientManager = mqttClientManager;
        this.mqttProperties = mqttProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(DeviceCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Device command is required");
        }

        String topicTemplate = validateTopicTemplate(mqttProperties.getCommandTopicTemplate());
        String topic = String.format(topicTemplate, command.macAddress());
        String payload = serialize(command);

        mqttClientManager.connect();
        mqttClientManager.publish(topic, payload, mqttProperties.getDefaultQos(), false);
    }

    private String serialize(DeviceCommand command) {
        try {
            return objectMapper.writeValueAsString(command);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize the device command payload", exception);
        }
    }

    private String validateTopicTemplate(String commandTopicTemplate) {
        if (commandTopicTemplate == null || commandTopicTemplate.isBlank()) {
            throw new IllegalArgumentException("MQTT command topic template is required");
        }
        if (!commandTopicTemplate.contains("%s")) {
            throw new IllegalArgumentException("MQTT command topic template must contain a %s placeholder");
        }
        return commandTopicTemplate.trim();
    }
}
