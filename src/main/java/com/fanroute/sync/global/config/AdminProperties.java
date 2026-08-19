package com.fanroute.sync.global.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** 소셜 로그인 시 부트스트랩 이메일 목록을 기준으로 관리자 권한을 동기화합니다. */
@Validated
@ConfigurationProperties(prefix = "admin")
public record AdminProperties(List<String> bootstrapEmails) {

  public AdminProperties {
    bootstrapEmails = bootstrapEmails == null ? List.of() : bootstrapEmails;
  }

  public boolean isBootstrapAdmin(String email) {
    return email != null
        && bootstrapEmails.stream().anyMatch(email::equalsIgnoreCase);
  }
}
