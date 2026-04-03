package com.neurolive.neuro_live_backend.repository;

import com.neurolive.neuro_live_backend.domain.biometric.BaseLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// Repositorio JPA para persistir y consultar lineas base biometricas.
public interface BaseLineRepository extends JpaRepository<BaseLine, Long> {

    Optional<BaseLine> findByPatientId(Long patientId);
}
