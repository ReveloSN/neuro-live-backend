package com.neurolive.neuro_live_backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/")
    public Map<String, String> root() {
        return Map.of(
                "name", "NeuroLive Backend",
                "status", "UP",
                "message", "NeuroLive backend is running",
                "health", "/health"
        );
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of(
                "status", "UP",
                "message", "NeuroLive backend running"
        );
    }
}
