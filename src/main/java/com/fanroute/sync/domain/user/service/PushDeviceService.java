package com.fanroute.sync.domain.user.service;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fanroute.sync.domain.user.entity.PushDevice;
import com.fanroute.sync.domain.user.entity.User;
import com.fanroute.sync.domain.user.repository.PushDeviceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PushDeviceService {
  private final PushDeviceRepository repository;

  @Transactional
  public void register(User user, String registrationToken) {
    Instant now = Instant.now();
    repository.findByRegistrationToken(registrationToken)
        .ifPresentOrElse(device -> device.refresh(user, now),
            () -> repository.save(PushDevice.create(user, registrationToken, now)));
  }

  @Transactional
  public void unregister(User user, String registrationToken) {
    repository.findByRegistrationToken(registrationToken)
        .filter(device -> device.getUser().getId().equals(user.getId()))
        .ifPresent(PushDevice::disable);
  }
}
