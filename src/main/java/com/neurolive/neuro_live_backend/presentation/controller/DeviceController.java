package com.neurolive.neuro_live_backend.presentation.controller;

import com.neurolive.neuro_live_backend.business.service.DeviceService;
import com.neurolive.neuro_live_backend.presentation.dto.DeviceLinkRequestDTO;
import com.neurolive.neuro_live_backend.presentation.dto.DeviceResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/devices")
// Expone la vinculacion explicita entre un paciente y su ESP32.
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    // Cierra RF05 reutilizando la regla de autorizacion clinica y el servicio de dispositivos.
    @PostMapping("/patients/{patientId}/link")
    public ResponseEntity<DeviceResponseDTO> linkDevice(Authentication authentication,
                                                        @PathVariable Long patientId,
                                                        @Valid @RequestBody DeviceLinkRequestDTO request,
                                                        HttpServletRequest httpServletRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                DeviceResponseDTO.from(
                        deviceService.linkDevice(
                                authentication.getName(),
                                patientId,
                                request.deviceMac(),
                                request.fallBackConfig(),
                                resolveIp(httpServletRequest)
                        )
                )
        );
    }

    private String resolveIp(HttpServletRequest httpServletRequest) {
        return httpServletRequest == null ? "unknown" : httpServletRequest.getRemoteAddr();
    }
}
