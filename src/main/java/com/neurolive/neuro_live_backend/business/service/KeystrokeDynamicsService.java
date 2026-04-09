package com.neurolive.neuro_live_backend.business.service;

import com.neurolive.neuro_live_backend.domain.analysis.KeystrokeDynamics;
import com.neurolive.neuro_live_backend.domain.user.User;
import com.neurolive.neuro_live_backend.repository.KeystrokeDynamicsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class KeystrokeDynamicsService {

    private final KeystrokeDynamicsRepository keystrokeDynamicsRepository;
    private final ClinicalAccessService clinicalAccessService;
    private final MonitoringConsentService monitoringConsentService;

    public KeystrokeDynamicsService(KeystrokeDynamicsRepository keystrokeDynamicsRepository,
                                    ClinicalAccessService clinicalAccessService,
                                    MonitoringConsentService monitoringConsentService) {
        this.keystrokeDynamicsRepository = keystrokeDynamicsRepository;
        this.clinicalAccessService = clinicalAccessService;
        this.monitoringConsentService = monitoringConsentService;
    }

    public KeystrokeDynamics capture(String requesterEmail,
                                     Long userId,
                                     String sessionId,
                                     Float dwellTime,
                                     Float flightTime,
                                     Integer errorCount,
                                     Float errorRate,
                                     LocalDateTime timestamp) {
        User requester = clinicalAccessService.resolveCurrentUser(requesterEmail);
        if (!requester.getId().equals(userId)) {
            clinicalAccessService.requirePatientAccess(requesterEmail, userId);
        }

        monitoringConsentService.assertMonitoringAllowed(userId);

        KeystrokeDynamics keystrokeDynamics = KeystrokeDynamics.capture(
                userId,
                sessionId,
                dwellTime,
                flightTime,
                errorCount,
                errorRate,
                timestamp
        );

        return keystrokeDynamicsRepository.save(keystrokeDynamics);
    }

    @Transactional(readOnly = true)
    public Optional<KeystrokeDynamics> findLatestForUser(Long userId) {
        return keystrokeDynamicsRepository.findTopByUserIdOrderByTimestampDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<KeystrokeDynamics> findRecentForUser(Long userId, int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("Limit must be greater than zero");
        }

        return keystrokeDynamicsRepository.findAllByUserIdOrderByTimestampDesc(userId).stream()
                .limit(limit)
                .toList();
    }
}
