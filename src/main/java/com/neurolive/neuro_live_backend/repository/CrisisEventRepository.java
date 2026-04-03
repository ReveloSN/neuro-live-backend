package com.neurolive.neuro_live_backend.repository;

import com.neurolive.neuro_live_backend.domain.crisis.CrisisEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// Repositorio JPA para persistir y consultar eventos de crisis.
public interface CrisisEventRepository extends JpaRepository<CrisisEvent, Long> {

    List<CrisisEvent> findAllByPatientIdOrderByStartedAtDesc(Long patientId);

    Optional<CrisisEvent> findFirstByPatientIdAndEndedAtIsNullOrderByStartedAtDesc(Long patientId);
}
