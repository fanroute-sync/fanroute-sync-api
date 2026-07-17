package com.fanroute.sync.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Repository 계층의 테스트에서 사용할 설정 모음
 * Spring ApplicationContext의 생명주기에 맞춰 컨테이너를 관리
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestContainerConfig {
  /**
   * PostgreSQL 컨테이너를 Spring Bean으로 등록
   * {@link ServiceConnection}을 통해 컨테이너의 JDBC URL, 사용자명, 비밀번호 등의 연결 정보를 Spring Boot가 자동으로 DataSource에 적용
   */
  @Bean
  @ServiceConnection
  PostgreSQLContainer postgresContainer() {
    return new PostgreSQLContainer("postgres:17-alpine");
  }
}