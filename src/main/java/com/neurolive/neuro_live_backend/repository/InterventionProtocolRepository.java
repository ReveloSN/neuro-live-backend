package com.neurolive.neuro_live_backend.repository;

import com.neurolive.neuro_live_backend.domain.crisis.InterventionProtocol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InterventionProtocolRepository extends JpaRepository<InterventionProtocol, Long> {

    Optional<InterventionProtocol> findByCrisisEvent_Id(Long crisisEventId);
}
