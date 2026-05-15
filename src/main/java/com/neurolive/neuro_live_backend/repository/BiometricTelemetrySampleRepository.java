package com.neurolive.neuro_live_backend.repository;

import com.neurolive.neuro_live_backend.domain.biometric.BiometricTelemetrySample;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

// Repositorio para almacenar y consultar muestras biometricas crudas.
public interface BiometricTelemetrySampleRepository extends JpaRepository<BiometricTelemetrySample, Long> {

    List<BiometricTelemetrySample> findAllByPatientIdOrderByObservedAtAsc(Long patientId);

    List<BiometricTelemetrySample> findAllByPatientIdAndObservedAtBetweenOrderByObservedAtAsc(
            Long patientId,
            LocalDateTime start,
            LocalDateTime end
    );
}
