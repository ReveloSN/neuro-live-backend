package com.neurolive.neuro_live_backend.presentation.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neurolive.neuro_live_backend.business.service.DeviceConnectionMonitorService;
import com.neurolive.neuro_live_backend.business.service.DeviceService;
import com.neurolive.neuro_live_backend.business.service.TelemetryIngestionResult;
import com.neurolive.neuro_live_backend.business.service.TelemetryIngestionService;
import com.neurolive.neuro_live_backend.domain.biometric.Device;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

// Verifica los endpoints internos consumidos por el WS Service.
class InternalControllerTest {

    private final TelemetryIngestionService telemetryIngestionService = Mockito.mock(TelemetryIngestionService.class);
    private final DeviceConnectionMonitorService deviceConnectionMonitorService = Mockito.mock(DeviceConnectionMonitorService.class);
    private final DeviceService deviceService = Mockito.mock(DeviceService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;

    // Prepara el controlador aislado para pruebas HTTP.
    @BeforeEach
    void setUp() {
        InternalController controller = new InternalController(
                telemetryIngestionService,
                deviceConnectionMonitorService,
                deviceService
        );
        ReflectionTestUtils.setField(controller, "internalToken", "test-internal-token");
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // Comprueba que la telemetria interna acepta el token correcto.
    @Test
    void shouldAcceptTelemetryWithValidInternalToken() throws Exception {
        Device device = new Device();
        device.register(81L, "AA:BB:CC:DD:EE:81", null);
        when(deviceService.findByMacAddress("AA:BB:CC:DD:EE:81")).thenReturn(device);
        when(telemetryIngestionService.ingest(any())).thenReturn(Mockito.mock(TelemetryIngestionResult.class));

        mockMvc.perform(post("/internal/telemetry")
                        .header("X-Internal-Token", "test-internal-token")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "deviceId", "AA:BB:CC:DD:EE:81",
                                "bpm", 90,
                                "spo2", 98,
                                "sensorConnected", true,
                                "receivedAt", "2026-04-22T00:00:00Z"
                        ))))
                .andExpect(status().isOk());
    }

    // Comprueba que la telemetria interna rechaza requests sin token.
    @Test
    void shouldRejectTelemetryWithoutInternalToken() throws Exception {
        mockMvc.perform(post("/internal/telemetry")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "deviceId", "AA:BB:CC:DD:EE:81",
                                "bpm", 90,
                                "spo2", 98
                        ))))
                .andExpect(status().isForbidden());
    }
}
