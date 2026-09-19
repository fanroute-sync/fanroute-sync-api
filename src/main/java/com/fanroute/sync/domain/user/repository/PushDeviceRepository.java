package com.fanroute.sync.domain.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fanroute.sync.domain.user.entity.PushDevice;

public interface PushDeviceRepository extends JpaRepository<PushDevice, Long> {
  Optional<PushDevice> findByRegistrationToken(String registrationToken);
}
