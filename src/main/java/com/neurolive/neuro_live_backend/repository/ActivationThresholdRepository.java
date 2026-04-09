package com.neurolive.neuro_live_backend.repository;

import com.neurolive.neuro_live_backend.domain.biometric.ActivationThreshold;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// Repositorio para consultar umbrales activos durante la evaluacion.
public interface ActivationThresholdRepository extends JpaRepository<ActivationThreshold, Long> {

    Optional<ActivationThreshold> findFirstByActiveTrueOrderByCreatedAtDesc();

    Optional<ActivationThreshold> findFirstByPatientIdAndActiveTrueOrderByCreatedAtDesc(Long patientId);

    Optional<ActivationThreshold> findFirstByPersonalUserIdAndActiveTrueOrderByCreatedAtDesc(Long personalUserId);

    Optional<ActivationThreshold> findFirstByPatientIdIsNullAndPersonalUserIdIsNullAndActiveTrueOrderByCreatedAtDesc();
}
