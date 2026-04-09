package com.neurolive.neuro_live_backend.business.service;

import com.neurolive.neuro_live_backend.data.enums.RoleEnum;
import com.neurolive.neuro_live_backend.data.exception.UnauthorizedAccessException;
import com.neurolive.neuro_live_backend.domain.biometric.ActivationThreshold;
import com.neurolive.neuro_live_backend.domain.user.PersonalUser;
import com.neurolive.neuro_live_backend.domain.user.User;
import com.neurolive.neuro_live_backend.repository.ActivationThresholdRepository;
import com.neurolive.neuro_live_backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ActivationThresholdService {

    private final ActivationThresholdRepository activationThresholdRepository;
    private final UserRepository userRepository;
    private final ClinicalAccessService clinicalAccessService;

    public ActivationThresholdService(ActivationThresholdRepository activationThresholdRepository,
                                      UserRepository userRepository,
                                      ClinicalAccessService clinicalAccessService) {
        this.activationThresholdRepository = activationThresholdRepository;
        this.userRepository = userRepository;
        this.clinicalAccessService = clinicalAccessService;
    }

    public ActivationThreshold saveForPatient(String requesterEmail,
                                              Long patientId,
                                              Float bpmMin,
                                              Float bpmMax,
                                              Float spo2Min,
                                              Float errorRateMax) {
        User requester = clinicalAccessService.requireThresholdManagement(requesterEmail, patientId);
        if (requester.getRole() != RoleEnum.DOCTOR) {
            throw new UnauthorizedAccessException("Only doctors can define thresholds for patients");
        }

        activationThresholdRepository.findFirstByPatientIdAndActiveTrueOrderByCreatedAtDesc(patientId)
                .ifPresent(existingThreshold -> {
                    existingThreshold.deactivate();
                    activationThresholdRepository.save(existingThreshold);
                });

        ActivationThreshold threshold = new ActivationThreshold(bpmMin, bpmMax, spo2Min, errorRateMax)
                .assignToPatient(patientId, requester.getId());

        return activationThresholdRepository.save(threshold);
    }

    public ActivationThreshold saveForCurrentPersonalUser(String requesterEmail,
                                                          Float bpmMin,
                                                          Float bpmMax,
                                                          Float spo2Min,
                                                          Float errorRateMax) {
        User requester = clinicalAccessService.resolveCurrentUser(requesterEmail);
        if (!(requester instanceof PersonalUser personalUser)) {
            throw new UnauthorizedAccessException("Only personal users can define self thresholds");
        }

        ActivationThreshold threshold = new ActivationThreshold(bpmMin, bpmMax, spo2Min, errorRateMax)
                .assignToPersonalUser(personalUser.getId(), personalUser.getId());
        personalUser.setThreshold(threshold);

        PersonalUser savedUser = (PersonalUser) userRepository.save(personalUser);
        return savedUser.getCustomThreshold();
    }

    @Transactional(readOnly = true)
    public ActivationThreshold resolveForPatient(Long patientId) {
        return activationThresholdRepository.findFirstByPatientIdAndActiveTrueOrderByCreatedAtDesc(patientId)
                .or(() -> activationThresholdRepository
                        .findFirstByPatientIdIsNullAndPersonalUserIdIsNullAndActiveTrueOrderByCreatedAtDesc())
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public ActivationThreshold resolveForPersonalUser(Long personalUserId) {
        return activationThresholdRepository.findFirstByPersonalUserIdAndActiveTrueOrderByCreatedAtDesc(personalUserId)
                .or(() -> activationThresholdRepository
                        .findFirstByPatientIdIsNullAndPersonalUserIdIsNullAndActiveTrueOrderByCreatedAtDesc())
                .orElse(null);
    }
}
