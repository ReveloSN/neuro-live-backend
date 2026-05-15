package com.neurolive.neuro_live_backend.presentation.controller;

import com.neurolive.neuro_live_backend.business.service.DeviceService;
import com.neurolive.neuro_live_backend.presentation.dto.DeviceLinkRequestDTO;
import com.neurolive.neuro_live_backend.presentation.dto.DeviceProvisioningResponseDTO;
import com.neurolive.neuro_live_backend.presentation.dto.DeviceResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/devices")
// Expone la vinculacion y consulta de dispositivos ESP32 asociados a pacientes.
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    // Vincula un nuevo ESP32 al paciente. El token de aprovisionamiento se expone solo aqui.
    @PostMapping("/patients/{patientId}/link")
    public ResponseEntity<DeviceProvisioningResponseDTO> linkDevice(Authentication authentication,
            @PathVariable Long patientId,
            @Valid @RequestBody DeviceLinkRequestDTO request,
            HttpServletRequest httpServletRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                DeviceProvisioningResponseDTO.from(
                        deviceService.linkDevice(
                                authentication.getName(),
                                patientId,
                                request.deviceMac(),
                                request.fallBackConfig(),
                                request.deviceToken(),
                                resolveIp(httpServletRequest))));
    }

    // Lista todos los dispositivos vinculados a un paciente.
    @GetMapping("/patients/{patientId}")
    public ResponseEntity<List<DeviceResponseDTO>> listDevices(Authentication authentication,
            @PathVariable Long patientId) {
        List<DeviceResponseDTO> devices = deviceService.findByPatientId(patientId).stream()
                .map(DeviceResponseDTO::from)
                .toList();
        return ResponseEntity.ok(devices);
    }

    // Devuelve el estado actual de un dispositivo especifico por MAC.
    @GetMapping("/patients/{patientId}/{mac}/status")
    public ResponseEntity<DeviceResponseDTO> getDeviceStatus(Authentication authentication,
            @PathVariable Long patientId,
            @PathVariable String mac) {
        return ResponseEntity.ok(DeviceResponseDTO.from(deviceService.findByMacAddress(mac)));
    }

    // Desvincula el dispositivo del paciente. Requiere acceso de gestion.
    @DeleteMapping("/patients/{patientId}/{mac}")
    public ResponseEntity<Void> unlinkDevice(Authentication authentication,
            @PathVariable Long patientId,
            @PathVariable String mac,
            HttpServletRequest httpServletRequest) {
        deviceService.unlinkDevice(authentication.getName(), patientId, mac, resolveIp(httpServletRequest));
        return ResponseEntity.noContent().build();
    }

    private String resolveIp(HttpServletRequest httpServletRequest) {
        return httpServletRequest == null ? "unknown" : httpServletRequest.getRemoteAddr();
    }
}
