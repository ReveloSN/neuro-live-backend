package com.neurolive.neuro_live_backend.presentation.controller;

import com.neurolive.neuro_live_backend.business.service.DeviceConnectionMonitorService;
import com.neurolive.neuro_live_backend.business.service.DeviceService;
import com.neurolive.neuro_live_backend.business.service.TelemetryIngestionService;
import com.neurolive.neuro_live_backend.presentation.dto.TelemetryPayload;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

// Expone endpoints internos consumidos solo por el WS Service.
@RestController
@RequestMapping("/internal")
public class InternalController {

    private final TelemetryIngestionService telemetryIngestionService;
    private final DeviceConnectionMonitorService deviceConnectionMonitorService;
    private final DeviceService deviceService;

    @Value("${internal.token:ws-internal-secret-change-in-prod}")
    private String internalToken;

    // Recibe los servicios usados por la integracion interna.
    public InternalController(TelemetryIngestionService telemetryIngestionService,
                              DeviceConnectionMonitorService deviceConnectionMonitorService,
                              DeviceService deviceService) {
        this.telemetryIngestionService = telemetryIngestionService;
        this.deviceConnectionMonitorService = deviceConnectionMonitorService;
        this.deviceService = deviceService;
    }

    // Recibe telemetria del WS Service y la ingiere en el pipeline actual.
    @PostMapping("/telemetry")
    public ResponseEntity<Void> receiveTelemetry(
            @RequestHeader(value = "X-Internal-Token", required = false) String token,
            @RequestBody Map<String, Object> body) {
        validateToken(token);
        TelemetryPayload payload = mapToTelemetryPayload(body);
        telemetryIngestionService.ingest(payload);
        return ResponseEntity.ok().build();
    }

    // Recibe la notificacion de desconexion desde el WS Service.
    @PostMapping("/devices/{deviceId}/disconnected")
    public ResponseEntity<Void> notifyDisconnect(
            @RequestHeader(value = "X-Internal-Token", required = false) String token,
            @PathVariable String deviceId,
            @RequestBody(required = false) Map<String, String> body) {
        validateToken(token);
        String reason = body == null ? "WebSocket closed" : body.getOrDefault("reason", "WebSocket closed");
        deviceConnectionMonitorService.handleDeviceDisconnected(deviceId, reason);
        return ResponseEntity.ok().build();
    }

    // Recibe la notificacion de autenticacion exitosa desde el WS Service.
    @PostMapping("/devices/{deviceId}/authenticated")
    public ResponseEntity<Void> notifyAuthenticated(
            @RequestHeader(value = "X-Internal-Token", required = false) String token,
            @PathVariable String deviceId,
            @RequestBody(required = false) Map<String, String> body) {
        validateToken(token);
        deviceConnectionMonitorService.handleDeviceConnected(deviceId);
        return ResponseEntity.ok().build();
    }

    // Permite al WS Service validar si un deviceId es aceptable.
    @PostMapping("/devices/validate-token")
    public ResponseEntity<Boolean> validateDeviceToken(
            @RequestHeader(value = "X-Internal-Token", required = false) String token,
            @RequestBody Map<String, String> body) {
        validateToken(token);
        String deviceId = body.get("deviceId");
        String deviceToken = body.get("token");
        boolean valid = deviceConnectionMonitorService.isValidDeviceToken(deviceId, deviceToken);
        return ResponseEntity.ok(valid);
    }

    // Verifica el token interno compartido entre servicios.
    private void validateToken(String token) {
        if (token == null || !internalToken.equals(token)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid internal token");
        }
    }

    // Mapea el cuerpo HTTP al payload que ya consume el negocio.
    private TelemetryPayload mapToTelemetryPayload(Map<String, Object> body) {
        String deviceMac = (String) body.get("deviceId");
        Long patientId = resolvePatientId(deviceMac);
        Float bpm = body.get("bpm") != null ? ((Number) body.get("bpm")).floatValue() : null;
        Float spo2 = body.get("spo2") != null ? ((Number) body.get("spo2")).floatValue() : null;
        Boolean sensorContact = body.get("sensorConnected") instanceof Boolean sensorConnected ? sensorConnected : null;
        LocalDateTime observedAt = resolveObservedAt(body);
        return new TelemetryPayload(patientId, deviceMac, bpm, spo2, observedAt, sensorContact);
    }

    // Resuelve el paciente vinculado a partir del MAC del dispositivo.
    private Long resolvePatientId(String deviceMac) {
        try {
            return deviceService.findByMacAddress(deviceMac).getPatientId();
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown deviceId");
        }
    }

    // Resuelve una fecha util usando receivedAt si fue enviada.
    private LocalDateTime resolveObservedAt(Map<String, Object> body) {
        Object receivedAt = body.get("receivedAt");
        if (receivedAt instanceof String value && !value.isBlank()) {
            try {
                return OffsetDateTime.parse(value).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
            } catch (Exception ignored) {
                // Mantiene un fallback simple si el timestamp no puede parsearse.
            }
        }
        return LocalDateTime.now();
    }
}

