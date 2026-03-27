package com.neurolive.neuro_live_backend.repository;

import com.neurolive.neuro_live_backend.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, Long> {

    boolean existsByMacAddress(String macAddress);

    Optional<Device> findByMacAddress(String macAddress);

    List<Device> findAllByPatientId(Long patientId);

    List<Device> findAllByIsConnectedTrue();
}
