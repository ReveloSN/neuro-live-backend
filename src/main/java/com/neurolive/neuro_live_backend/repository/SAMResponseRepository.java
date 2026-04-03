package com.neurolive.neuro_live_backend.repository;

import com.neurolive.neuro_live_backend.domain.crisis.SAMResponse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SAMResponseRepository extends JpaRepository<SAMResponse, Long> {

    Optional<SAMResponse> findByCrisisEvent_Id(Long crisisEventId);

    List<SAMResponse> findAllByPatientIdOrderByRecordedAtDesc(Long patientId);
}
