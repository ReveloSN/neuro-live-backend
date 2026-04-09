package com.neurolive.neuro_live_backend.infrastructure.config;

import com.neurolive.neuro_live_backend.infrastructure.mqtt.MQTTProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MQTTProperties.class)
// Centraliza el binding de propiedades MQTT y deja QoS 1 como contrato por defecto.
public class MQTTConfig {
}
