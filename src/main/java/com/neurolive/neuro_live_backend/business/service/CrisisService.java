package com.neurolive.neuro_live_backend.business.service;

import com.neurolive.neuro_live_backend.data.enums.StateEnum;
import com.neurolive.neuro_live_backend.data.enums.TypeEnum;
import com.neurolive.neuro_live_backend.data.exception.CrisisNotFoundException;
import com.neurolive.neuro_live_backend.domain.biometric.BaseLine;
import com.neurolive.neuro_live_backend.domain.biometric.BiometricTelemetrySample;
import com.neurolive.neuro_live_backend.domain.crisis.CrisisEvent;
import com.neurolive.neuro_live_backend.domain.crisis.SAMResponse;
import com.neurolive.neuro_live_backend.domain.user.User;
import com.neurolive.neuro_live_backend.repository.BiometricTelemetrySampleRepository;
import com.neurolive.neuro_live_backend.repository.CrisisEventRepository;
import com.neurolive.neuro_live_backend.repository.SAMResponseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class CrisisService {

    private static final Duration DEFAULT_QUERY_RANGE = Duration.ofDays(30);

    private final CrisisEventRepository crisisEventRepository;
    private final SAMResponseRepository samResponseRepository;
    private final ClinicalAccessService clinicalAccessService;
    private final AuditLogService auditLogService;
    private final BaseLineService baseLineService;
    private final BiometricTelemetrySampleRepository biometricTelemetrySampleRepository;
    private final ClinicalInsightService clinicalInsightService;

    public CrisisService(CrisisEventRepository crisisEventRepository,
                         SAMResponseRepository samResponseRepository,
                         ClinicalAccessService clinicalAccessService,
                         AuditLogService auditLogService,
                         BaseLineService baseLineService,
                         BiometricTelemetrySampleRepository biometricTelemetrySampleRepository,
                         ClinicalInsightService clinicalInsightService) {
        this.crisisEventRepository = crisisEventRepository;
        this.samResponseRepository = samResponseRepository;
        this.clinicalAccessService = clinicalAccessService;
        this.auditLogService = auditLogService;
        this.baseLineService = baseLineService;
        this.biometricTelemetrySampleRepository = biometricTelemetrySampleRepository;
        this.clinicalInsightService = clinicalInsightService;
    }

    @Transactional(readOnly = true)
    public CrisisEvent getCrisis(String requesterEmail, Long crisisId, String ipOrigin) {
        CrisisEvent crisisEvent = getCrisisOrThrow(crisisId);
        User requester = clinicalAccessService.requirePatientAccess(requesterEmail, crisisEvent.getPatientId());
        auditLogService.record(requester.getId(), "READ_CRISIS_EVENT", crisisEvent.getPatientId(), ipOrigin);
        return crisisEvent;
    }

    @Transactional(readOnly = true)
    public List<CrisisEvent> getCrisesByPatient(String requesterEmail,
                                                Long patientId,
                                                LocalDateTime start,
                                                LocalDateTime end,
                                                String ipOrigin) {
        User requester = clinicalAccessService.requirePatientAccess(requesterEmail, patientId);
        DateRange dateRange = normalizeDateRange(start, end);
        auditLogService.record(requester.getId(), "READ_CRISIS_LIST", patientId, ipOrigin);
        return crisisEventRepository.findAllByPatientIdAndStartedAtBetweenOrderByStartedAtDesc(
                patientId,
                dateRange.start(),
                dateRange.end()
        );
    }

    public CrisisEvent closeCrisis(String requesterEmail,
                                   Long crisisId,
                                   LocalDateTime endedAt,
                                   StateEnum finalState,
                                   String ipOrigin) {
        CrisisEvent crisisEvent = getCrisisOrThrow(crisisId);
        User requester = clinicalAccessService.requirePatientAccess(requesterEmail, crisisEvent.getPatientId());

        boolean wasActive = crisisEvent.isActive();
        if (wasActive) {
            crisisEvent.close(
                    endedAt == null ? LocalDateTime.now() : endedAt,
                    finalState == null ? StateEnum.NORMAL : finalState,
                    resolveInterventionType(crisisEvent)
            );
        }

        auditLogService.record(requester.getId(), "CLOSE_CRISIS_EVENT", crisisEvent.getPatientId(), ipOrigin);
        CrisisEvent savedEvent = crisisEventRepository.save(crisisEvent);
        if (wasActive) {
            clinicalInsightService.generatePostCrisisSummaryAsync(savedEvent);
        }
        return savedEvent;
    }

    public SAMResponse recordSamResponse(String requesterEmail,
                                         Long crisisId,
                                         Integer valence,
                                         Integer arousal,
                                         String ipOrigin) {
        CrisisEvent crisisEvent = getCrisisOrThrow(crisisId);
        User requester = clinicalAccessService.requirePatientAccess(requesterEmail, crisisEvent.getPatientId());

        boolean wasActive = crisisEvent.isActive();
        if (wasActive) {
            crisisEvent.close(LocalDateTime.now(), StateEnum.NORMAL, resolveInterventionType(crisisEvent));
        }

        SAMResponse samResponse = SAMResponse.create(crisisEvent, valence, arousal, LocalDateTime.now());
        crisisEvent.attachSamResponse(samResponse);
        CrisisEvent savedEvent = crisisEventRepository.save(crisisEvent);
        auditLogService.record(requester.getId(), "REGISTER_SAM_RESPONSE", crisisEvent.getPatientId(), ipOrigin);
        if (wasActive) {
            clinicalInsightService.generatePostCrisisSummaryAsync(savedEvent);
        }
        return savedEvent.getSamResponse();
    }

    @Transactional(readOnly = true)
    public SAMResponse getSamResponse(String requesterEmail, Long crisisId, String ipOrigin) {
        CrisisEvent crisisEvent = getCrisisOrThrow(crisisId);
        User requester = clinicalAccessService.requirePatientAccess(requesterEmail, crisisEvent.getPatientId());
        auditLogService.record(requester.getId(), "READ_SAM_RESPONSE", crisisEvent.getPatientId(), ipOrigin);
        return samResponseRepository.findByCrisisEvent_Id(crisisId)
                .orElseThrow(() -> new IllegalArgumentException("SAM response not found for crisis " + crisisId));
    }

    @Transactional(readOnly = true)
    public ClinicalAnalysisSnapshot buildAnalysis(String requesterEmail,
                                                  Long patientId,
                                                  LocalDateTime start,
                                                  LocalDateTime end,
                                                  String ipOrigin) {
        List<CrisisEvent> crisisEvents = getCrisesByPatient(requesterEmail, patientId, start, end, ipOrigin);
        BaseLine baseLine = null;
        try {
            baseLine = baseLineService.findByPatientId(patientId);
        } catch (RuntimeException ignored) {
            baseLine = null;
        }

        long activeEvents = crisisEvents.stream().filter(CrisisEvent::isActive).count();
        double averageDurationSeconds = crisisEvents.stream()
                .map(CrisisEvent::calculateDuration)
                .mapToLong(Duration::getSeconds)
                .average()
                .orElse(0.0d);
        double averageSamValence = crisisEvents.stream()
                .filter(event -> event.getSamValence() != null)
                .mapToInt(CrisisEvent::getSamValence)
                .average()
                .orElse(0.0d);
        double averageSamArousal = crisisEvents.stream()
                .filter(event -> event.getSamArousal() != null)
                .mapToInt(CrisisEvent::getSamArousal)
                .average()
                .orElse(0.0d);

        Map<TypeEnum, Long> interventionCounts = new EnumMap<>(TypeEnum.class);
        crisisEvents.stream()
                .map(CrisisEvent::getInterventionType)
                .filter(java.util.Objects::nonNull)
                // Cuenta las intervenciones por familia clinica para que el analisis salga limpio y comparable.
                .map(TypeEnum::canonical)
                .forEach(type -> interventionCounts.merge(type, 1L, Long::sum));

        return new ClinicalAnalysisSnapshot(
                patientId,
                crisisEvents.size(),
                activeEvents,
                averageDurationSeconds,
                averageSamValence,
                averageSamArousal,
                interventionCounts,
                baseLine == null || !baseLine.isReady() ? null : baseLine.getAvgBpm(),
                baseLine == null || !baseLine.isReady() ? null : baseLine.getAvgSpo2()
        );
    }

    @Transactional(readOnly = true)
    public String exportCsv(String requesterEmail,
                            Long patientId,
                            LocalDateTime start,
                            LocalDateTime end,
                            String ipOrigin) {
        List<CrisisEvent> crisisEvents = getCrisesByPatient(requesterEmail, patientId, start, end, ipOrigin);
        StringBuilder csvBuilder = new StringBuilder();
        csvBuilder.append("crisisId,patientId,state,interventionType,startedAt,endedAt,durationSeconds,triggerBpm,triggerSpo2,typingErrorRate,samValence,samArousal")
                .append(System.lineSeparator());

        for (CrisisEvent crisisEvent : crisisEvents) {
            csvBuilder.append(crisisEvent.getId()).append(',')
                    .append(crisisEvent.getPatientId()).append(',')
                    .append(crisisEvent.getState()).append(',')
                    .append(crisisEvent.getInterventionType() == null
                            ? null
                            : crisisEvent.getInterventionType().canonical()).append(',')
                    .append(crisisEvent.getStartedAt()).append(',')
                    .append(crisisEvent.getEndedAt()).append(',')
                    .append(crisisEvent.calculateDuration().getSeconds()).append(',')
                    .append(crisisEvent.getTriggerBpm()).append(',')
                    .append(crisisEvent.getTriggerSpo2()).append(',')
                    .append(crisisEvent.getTypingErrorRate()).append(',')
                    .append(crisisEvent.getSamValence()).append(',')
                    .append(crisisEvent.getSamArousal())
                    .append(System.lineSeparator());
        }

        User requester = clinicalAccessService.requirePatientAccess(requesterEmail, patientId);
        auditLogService.record(requester.getId(), "EXPORT_CRISIS_CSV", patientId, ipOrigin);
        return csvBuilder.toString();
    }

    @Transactional(readOnly = true)
    // Construye una narrativa clinica reciente sin exponer datos personales.
    public ClinicalInsightSnapshot buildNarrativeInsight(String requesterEmail,
                                                         Long patientId,
                                                         Integer days,
                                                         String ipOrigin) {
        User requester = clinicalAccessService.requirePatientAccess(requesterEmail, patientId);
        DateRange dateRange = normalizeInsightRange(days);
        List<CrisisEvent> crisisEvents = crisisEventRepository.findAllByPatientIdAndStartedAtBetweenOrderByStartedAtDesc(
                patientId,
                dateRange.start(),
                dateRange.end()
        );
        List<BiometricTelemetrySample> telemetrySamples = biometricTelemetrySampleRepository
                .findAllByPatientIdOrderByObservedAtAsc(patientId)
                .stream()
                .filter(sample -> !sample.getObservedAt().isBefore(dateRange.start())
                        && !sample.getObservedAt().isAfter(dateRange.end()))
                .toList();
        auditLogService.record(requester.getId(), "READ_CLINICAL_AI_INSIGHT", patientId, ipOrigin);
        String insight = clinicalInsightService.generatePatientEvolutionInsight(
                patientId,
                crisisEvents,
                telemetrySamples,
                dateRange.start(),
                dateRange.end()
        );
        return new ClinicalInsightSnapshot(patientId, dateRange.start(), dateRange.end(), insight);
    }

    private CrisisEvent getCrisisOrThrow(Long crisisId) {
        if (crisisId == null || crisisId <= 0) {
            throw new IllegalArgumentException("Crisis identifier must be positive");
        }

        return crisisEventRepository.findById(crisisId)
                .orElseThrow(() -> new CrisisNotFoundException(crisisId));
    }

    private TypeEnum resolveInterventionType(CrisisEvent crisisEvent) {
        if (crisisEvent.getInterventionType() != null) {
            return crisisEvent.getInterventionType().canonical();
        }
        if (crisisEvent.getInterventionProtocol() != null) {
            return crisisEvent.getInterventionProtocol().getType().canonical();
        }
        return TypeEnum.NO_INTERVENTION;
    }

    private DateRange normalizeDateRange(LocalDateTime start, LocalDateTime end) {
        LocalDateTime effectiveEnd = end == null ? LocalDateTime.now() : end;
        LocalDateTime effectiveStart = start == null ? effectiveEnd.minus(DEFAULT_QUERY_RANGE) : start;

        if (effectiveEnd.isBefore(effectiveStart)) {
            throw new IllegalArgumentException("End date must be after the start date");
        }

        return new DateRange(effectiveStart, effectiveEnd);
    }

    // Limita la ventana del insight para mantener la consulta simple.
    private DateRange normalizeInsightRange(Integer days) {
        int safeDays = days == null || days <= 0 ? 7 : Math.min(days, 30);
        LocalDateTime end = LocalDateTime.now();
        return new DateRange(end.minusDays(safeDays), end);
    }

    public record ClinicalAnalysisSnapshot(
            Long patientId,
            long totalEvents,
            long activeEvents,
            double averageDurationSeconds,
            double averageSamValence,
            double averageSamArousal,
            Map<TypeEnum, Long> interventionCounts,
            Float baselineBpm,
            Float baselineSpo2
    ) {
    }

    public record ClinicalInsightSnapshot(
            Long patientId,
            LocalDateTime start,
            LocalDateTime end,
            String insight
    ) {
    }

    private record DateRange(LocalDateTime start, LocalDateTime end) {
    }
}
