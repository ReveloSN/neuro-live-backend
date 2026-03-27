package com.neurolive.neuro_live_backend.repository;

import com.neurolive.neuro_live_backend.entity.BaseLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BaseLineRepository extends JpaRepository<BaseLine, Long> {

    Optional<BaseLine> findByPatientId(Long patientId);
}
