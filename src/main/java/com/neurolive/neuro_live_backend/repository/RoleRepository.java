package com.neurolive.neuro_live_backend.repository;

import com.neurolive.neuro_live_backend.entity.Role;
import com.neurolive.neuro_live_backend.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);
}