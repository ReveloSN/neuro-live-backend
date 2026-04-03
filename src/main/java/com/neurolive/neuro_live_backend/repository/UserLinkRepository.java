package com.neurolive.neuro_live_backend.repository;

import com.neurolive.neuro_live_backend.data.enums.StatusEnum;
import com.neurolive.neuro_live_backend.domain.user.UserLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// Repositorio JPA para manejar vinculos entre pacientes y usuarios.
public interface UserLinkRepository extends JpaRepository<UserLink, Long> {

    Optional<UserLink> findByToken(String token);

    boolean existsByPatient_IdAndLinkedUser_IdAndStatus(Long patientId, Long linkedUserId, StatusEnum status);

    List<UserLink> findAllByPatient_IdAndStatus(Long patientId, StatusEnum status);
}
