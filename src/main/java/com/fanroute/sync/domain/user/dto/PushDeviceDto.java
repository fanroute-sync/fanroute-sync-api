package com.fanroute.sync.domain.user.dto;

import jakarta.validation.constraints.NotBlank;

public final class PushDeviceDto {
  private PushDeviceDto() {
  }

  public record RegisterRequest(@NotBlank String registrationToken) {
  }
}
