package com.fanroute.sync.support;

import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.fanroute.sync.global.config.JpaAuditingConfig;

/**
 * Repository 계층의 JPA 통합 테스트에서 공통으로 사용하는 추상 부모 클래스
 * 개별 Repository 테스트 시, 해당을 상속하여 Testcontainers, JPA Auditing 등 추가 설정을 생략
 */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import({ JpaAuditingConfig.class, TestContainerConfig.class })
public abstract class AbstractRepositoryTest {
}
