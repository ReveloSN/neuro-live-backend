package com.neurolive.neuro_live_backend.business.service;

import com.neurolive.neuro_live_backend.business.patterns.AuditoryRegulationStrategy;
import com.neurolive.neuro_live_backend.business.patterns.CrisisMediator;
import com.neurolive.neuro_live_backend.business.patterns.GuidedBreathingStrategy;
import com.neurolive.neuro_live_backend.business.patterns.LightingInterventionStrategy;
import com.neurolive.neuro_live_backend.business.patterns.PatientStateObserver;
import com.neurolive.neuro_live_backend.business.patterns.PatientStateUpdate;
import com.neurolive.neuro_live_backend.business.patterns.UiReductionStrategy;
import com.neurolive.neuro_live_backend.data.enums.StateEnum;
import com.neurolive.neuro_live_backend.domain.biometric.ActivationThreshold;
import com.neurolive.neuro_live_backend.domain.biometric.BaseLine;
import com.neurolive.neuro_live_backend.domain.biometric.BiometricData;
import com.neurolive.neuro_live_backend.domain.biometric.BiometricTelemetrySample;
import com.neurolive.neuro_live_backend.domain.biometric.Device;
import com.neurolive.neuro_live_backend.domain.crisis.EmotionalState;
import com.neurolive.neuro_live_backend.infrastructure.mqtt.TelemetryPayload;
import com.neurolive.neuro_live_backend.repository.ActivationThresholdRepository;
import com.neurolive.neuro_live_backend.repository.BiometricTelemetrySampleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelemetryIngestionServiceTest {

    @Mock
    private BiometricTelemetrySampleRepository biometricTelemetrySampleRepository;

    @Mock
    private DeviceService deviceService;

    @Mock
    private BaseLineService baseLineService;

    @Mock
    private ActivationThresholdRepository activationThresholdRepository;

    @Mock
    private CrisisMediator crisisMediator;

    @Mock
    private CrisisOutcomePersistenceService crisisOutcomePersistenceService;

    @InjectMocks
    private TelemetryIngestionService telemetryIngestionService;

    @Test
    void shouldPersistValidTelemetry() {
        TelemetryPayload payload = buildPayload(81L, "AA:BB:CC:DD:EE:41", 88.0f, 97.0f, LocalDateTime.of(2026, 4, 2, 10, 0));
        Device device = buildDevice(81L, payload.deviceMac());
        BaseLine baseLine = buildReadyBaseLine(81L, 80.0f, 98.0f);
        BiometricTelemetrySample storedSample = BiometricTelemetrySample.from(payload.patientId(), payload.deviceMac(), toDomain(payload));

        when(deviceService.findByMacAddress(payload.deviceMac())).thenReturn(device);
        when(biometricTelemetrySampleRepository.save(any(BiometricTelemetrySample.class))).thenReturn(storedSample);
        when(deviceService.registerTelemetry(payload.deviceMac(), payload.observedAt())).thenReturn(device);
        when(biometricTelemetrySampleRepository.findAllByPatientIdOrderByObservedAtAsc(payload.patientId())).thenReturn(List.of(storedSample));
        when(baseLineService.updateFromTelemetry(payload.patientId(), List.of(storedSample.toDomain()))).thenReturn(baseLine);
        when(activationThresholdRepository.findFirstByActiveTrueOrderByCreatedAtDesc()).thenReturn(Optional.empty());
        when(crisisMediator.mediate(any())).thenReturn(CrisisMediator.CrisisMediationResult.withoutCrisis(
                EmotionalState.from(StateEnum.NORMAL)
        ));

        TelemetryIngestionResult result = telemetryIngestionService.ingest(payload);

        assertEquals(storedSample, result.storedSample());
        assertEquals(device, result.updatedDevice());
        verify(biometricTelemetrySampleRepository).save(any(BiometricTelemetrySample.class));
    }

    @Test
    void shouldUpdateDeviceStatusOnValidTelemetry() {
        TelemetryPayload payload = buildPayload(82L, "AA:BB:CC:DD:EE:42", 90.0f, 97.0f, LocalDateTime.of(2026, 4, 2, 10, 5));
        Device device = buildDevice(82L, payload.deviceMac());
        BaseLine baseLine = buildReadyBaseLine(82L, 80.0f, 98.0f);
        BiometricTelemetrySample storedSample = BiometricTelemetrySample.from(payload.patientId(), payload.deviceMac(), toDomain(payload));

        when(deviceService.findByMacAddress(payload.deviceMac())).thenReturn(device);
        when(biometricTelemetrySampleRepository.save(any(BiometricTelemetrySample.class))).thenReturn(storedSample);
        when(deviceService.registerTelemetry(payload.deviceMac(), payload.observedAt())).thenReturn(device);
        when(biometricTelemetrySampleRepository.findAllByPatientIdOrderByObservedAtAsc(payload.patientId())).thenReturn(List.of(storedSample));
        when(baseLineService.updateFromTelemetry(payload.patientId(), List.of(storedSample.toDomain()))).thenReturn(baseLine);
        when(activationThresholdRepository.findFirstByActiveTrueOrderByCreatedAtDesc()).thenReturn(Optional.empty());
        when(crisisMediator.mediate(any())).thenReturn(CrisisMediator.CrisisMediationResult.withoutCrisis(
                EmotionalState.from(StateEnum.NORMAL)
        ));

        telemetryIngestionService.ingest(payload);

        verify(deviceService).registerTelemetry(payload.deviceMac(), payload.observedAt());
    }

    @Test
    void shouldCallCrisisMediatorWithBaselineThresholdContext() {
        TelemetryPayload payload = buildPayload(83L, "AA:BB:CC:DD:EE:43", 98.0f, 96.0f, LocalDateTime.of(2026, 4, 2, 10, 10));
        Device device = buildDevice(83L, payload.deviceMac());
        BaseLine baseLine = buildReadyBaseLine(83L, 80.0f, 98.0f);
        ActivationThreshold activationThreshold = new ActivationThreshold(null, 95.0f, null, null);
        BiometricTelemetrySample storedSample = BiometricTelemetrySample.from(payload.patientId(), payload.deviceMac(), toDomain(payload));

        when(deviceService.findByMacAddress(payload.deviceMac())).thenReturn(device);
        when(biometricTelemetrySampleRepository.save(any(BiometricTelemetrySample.class))).thenReturn(storedSample);
        when(deviceService.registerTelemetry(payload.deviceMac(), payload.observedAt())).thenReturn(device);
        when(biometricTelemetrySampleRepository.findAllByPatientIdOrderByObservedAtAsc(payload.patientId())).thenReturn(List.of(storedSample));
        when(baseLineService.updateFromTelemetry(payload.patientId(), List.of(storedSample.toDomain()))).thenReturn(baseLine);
        when(activationThresholdRepository.findFirstByActiveTrueOrderByCreatedAtDesc()).thenReturn(Optional.of(activationThreshold));
        when(crisisMediator.mediate(any())).thenReturn(CrisisMediator.CrisisMediationResult.withoutCrisis(
                EmotionalState.from(StateEnum.NORMAL)
        ));

        telemetryIngestionService.ingest(payload);

        ArgumentCaptor<CrisisMediator.CrisisEvaluationInput> inputCaptor =
                ArgumentCaptor.forClass(CrisisMediator.CrisisEvaluationInput.class);
        verify(crisisMediator).mediate(inputCaptor.capture());
        assertEquals(baseLine, inputCaptor.getValue().baseLine());
        assertEquals(activationThreshold, inputCaptor.getValue().activationThreshold());
        assertEquals(payload.patientId(), inputCaptor.getValue().patientId());
        assertEquals(payload.bpm(), inputCaptor.getValue().currentBiometricData().bpm());
    }

    @Test
    void shouldStillPersistDataWhenBaselineIsNotReady() {
        TelemetryPayload payload = buildPayload(84L, "AA:BB:CC:DD:EE:44", 84.0f, 98.0f, LocalDateTime.of(2026, 4, 2, 10, 15));
        Device device = buildDevice(84L, payload.deviceMac());
        BaseLine baseLine = new BaseLine(84L);
        BiometricTelemetrySample storedSample = BiometricTelemetrySample.from(payload.patientId(), payload.deviceMac(), toDomain(payload));

        when(deviceService.findByMacAddress(payload.deviceMac())).thenReturn(device);
        when(biometricTelemetrySampleRepository.save(any(BiometricTelemetrySample.class))).thenReturn(storedSample);
        when(deviceService.registerTelemetry(payload.deviceMac(), payload.observedAt())).thenReturn(device);
        when(biometricTelemetrySampleRepository.findAllByPatientIdOrderByObservedAtAsc(payload.patientId())).thenReturn(List.of(storedSample));
        when(baseLineService.updateFromTelemetry(payload.patientId(), List.of(storedSample.toDomain()))).thenReturn(baseLine);
        when(activationThresholdRepository.findFirstByActiveTrueOrderByCreatedAtDesc()).thenReturn(Optional.empty());

        TelemetryIngestionResult result = telemetryIngestionService.ingest(payload);

        assertEquals(storedSample, result.storedSample());
        assertNull(result.crisisMediationResult());
        verify(crisisMediator, never()).mediate(any());
    }

    @Test
    void shouldRejectInvalidTelemetry() {
        TelemetryPayload payload = new TelemetryPayload(null, "AA:BB:CC:DD:EE:45", 80.0f, 97.0f, LocalDateTime.now());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> telemetryIngestionService.ingest(payload)
        );

        assertEquals("Patient reference must be a positive identifier", exception.getMessage());
        verify(biometricTelemetrySampleRepository, never()).save(any());
    }

    @Test
    void shouldPersistCrisisOutcomeWhenMediatorDetectsCrisis() {
        TelemetryPayload payload = buildPayload(86L, "AA:BB:CC:DD:EE:47", 112.0f, 92.0f, LocalDateTime.of(2026, 4, 2, 10, 25));
        Device device = buildDevice(86L, payload.deviceMac());
        BaseLine baseLine = buildReadyBaseLine(86L, 80.0f, 98.0f);
        BiometricTelemetrySample storedSample = BiometricTelemetrySample.from(payload.patientId(), payload.deviceMac(), toDomain(payload));
        CrisisMediator.CrisisMediationResult mediationResult = buildCrisisResult(payload.patientId(), payload.observedAt());

        when(deviceService.findByMacAddress(payload.deviceMac())).thenReturn(device);
        when(biometricTelemetrySampleRepository.save(any(BiometricTelemetrySample.class))).thenReturn(storedSample);
        when(deviceService.registerTelemetry(payload.deviceMac(), payload.observedAt())).thenReturn(device);
        when(biometricTelemetrySampleRepository.findAllByPatientIdOrderByObservedAtAsc(payload.patientId())).thenReturn(List.of(storedSample));
        when(baseLineService.updateFromTelemetry(payload.patientId(), List.of(storedSample.toDomain()))).thenReturn(baseLine);
        when(activationThresholdRepository.findFirstByActiveTrueOrderByCreatedAtDesc()).thenReturn(Optional.empty());
        when(crisisMediator.mediate(any())).thenReturn(mediationResult);

        telemetryIngestionService.ingest(payload);

        verify(crisisOutcomePersistenceService).persist(mediationResult);
    }

    @Test
    void shouldNotifyObserversThroughExistingMediatorFlow() {
        BiometricTelemetrySampleRepository sampleRepository = org.mockito.Mockito.mock(BiometricTelemetrySampleRepository.class);
        DeviceService localDeviceService = org.mockito.Mockito.mock(DeviceService.class);
        BaseLineService localBaseLineService = org.mockito.Mockito.mock(BaseLineService.class);
        ActivationThresholdRepository localThresholdRepository = org.mockito.Mockito.mock(ActivationThresholdRepository.class);
        CrisisOutcomePersistenceService localCrisisOutcomePersistenceService =
                org.mockito.Mockito.mock(CrisisOutcomePersistenceService.class);
        RecordingObserver observer = new RecordingObserver();
        CrisisMediator actualMediator = new CrisisMediator(
                List.of(
                        new UiReductionStrategy(),
                        new GuidedBreathingStrategy(),
                        new LightingInterventionStrategy(),
                        new AuditoryRegulationStrategy()
                ),
                List.of(observer)
        );
        TelemetryIngestionService localService = new TelemetryIngestionService(
                sampleRepository,
                localDeviceService,
                localBaseLineService,
                localThresholdRepository,
                actualMediator,
                localCrisisOutcomePersistenceService
        );

        TelemetryPayload payload = buildPayload(85L, "AA:BB:CC:DD:EE:46", 96.0f, 96.0f, LocalDateTime.of(2026, 4, 2, 10, 20));
        Device device = buildDevice(85L, payload.deviceMac());
        BaseLine baseLine = buildReadyBaseLine(85L, 80.0f, 98.0f);
        BiometricTelemetrySample storedSample = BiometricTelemetrySample.from(payload.patientId(), payload.deviceMac(), toDomain(payload));

        when(localDeviceService.findByMacAddress(payload.deviceMac())).thenReturn(device);
        when(sampleRepository.save(any(BiometricTelemetrySample.class))).thenReturn(storedSample);
        when(localDeviceService.registerTelemetry(payload.deviceMac(), payload.observedAt())).thenReturn(device);
        when(sampleRepository.findAllByPatientIdOrderByObservedAtAsc(payload.patientId())).thenReturn(List.of(storedSample));
        when(localBaseLineService.updateFromTelemetry(payload.patientId(), List.of(storedSample.toDomain()))).thenReturn(baseLine);
        when(localThresholdRepository.findFirstByActiveTrueOrderByCreatedAtDesc()).thenReturn(Optional.empty());

        TelemetryIngestionResult result = localService.ingest(payload);

        assertNotNull(result.crisisMediationResult());
        assertEquals(1, observer.updates().size());
        assertTrue(observer.updates().getFirst().emotionalState().isAtRisk());
        assertEquals(payload.patientId(), observer.updates().getFirst().patientId());
    }

    private TelemetryPayload buildPayload(Long patientId, String deviceMac, Float bpm, Float spo2, LocalDateTime observedAt) {
        return new TelemetryPayload(patientId, deviceMac, bpm, spo2, observedAt);
    }

    private Device buildDevice(Long patientId, String deviceMac) {
        Device device = new Device();
        device.register(patientId, deviceMac, null);
        setDeviceId(device, 500L + patientId);
        return device;
    }

    private BaseLine buildReadyBaseLine(Long patientId, float avgBpm, float avgSpo2) {
        BaseLine baseLine = new BaseLine(patientId);
        LocalDateTime sessionStart = LocalDateTime.of(2026, 4, 2, 9, 0);

        baseLine.calculate(List.of(
                new BiometricData(avgBpm, avgSpo2, sessionStart),
                new BiometricData(avgBpm, avgSpo2, sessionStart.plusMinutes(1)),
                new BiometricData(avgBpm, avgSpo2, sessionStart.plusMinutes(2)),
                new BiometricData(avgBpm, avgSpo2, sessionStart.plusMinutes(3)),
                new BiometricData(avgBpm, avgSpo2, sessionStart.plusMinutes(4)),
                new BiometricData(avgBpm, avgSpo2, sessionStart.plusMinutes(5))
        ));

        return baseLine;
    }

    private BiometricData toDomain(TelemetryPayload payload) {
        return new BiometricData(payload.bpm(), payload.spo2(), payload.observedAt());
    }

    private CrisisMediator.CrisisMediationResult buildCrisisResult(Long patientId, LocalDateTime startedAt) {
        return CrisisMediator.CrisisMediationResult.crisisDetected(
                EmotionalState.from(StateEnum.ACTIVE_CRISIS),
                com.neurolive.neuro_live_backend.domain.crisis.CrisisEvent.open(
                        patientId,
                        StateEnum.ACTIVE_CRISIS,
                        startedAt
                ),
                com.neurolive.neuro_live_backend.domain.crisis.InterventionProtocol.builder(
                                com.neurolive.neuro_live_backend.data.enums.TypeEnum.GUIDED_BREATHING
                        )
                        .breathingEnabled()
                        .build()
        );
    }

    private void setDeviceId(Device device, Long id) {
        setField(Device.class, device, "id", id);
    }

    private void setField(Class<?> owner, Object target, String fieldName, Object value) {
        try {
            Field field = owner.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to set test field " + fieldName, exception);
        }
    }

    private static final class RecordingObserver implements PatientStateObserver {

        private final List<PatientStateUpdate> updates = new ArrayList<>();

        @Override
        public void onPatientStateChanged(PatientStateUpdate update) {
            updates.add(update);
        }

        private List<PatientStateUpdate> updates() {
            return updates;
        }
    }
}
