package com.fanroute.sync;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.fanroute.sync.support.TestContainerConfig;

@SpringBootTest(properties = {
    "auth.google.client-id=test-google-client",
    "auth.google.client-secret=test-google-secret",
    "auth.google.redirect-uri=http://localhost/test/callback",
    "auth.jwt.secret=dGVzdC1vbmx5LWtleS10aGF0LWlzLWF0LWxlYXN0LTMyLWJ5dGVzLWxvbmc=",
    "kopis.service-key=test-kopis-key",
    "tour-api.service-key=test-tour-key",
    "cors.allowed-origins=http://localhost",
    "spring.data.redis.host=localhost",
    "spring.data.redis.port=6379",
    "spring.data.redis.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import(TestContainerConfig.class)
class FanrouteSyncApiApplicationTests {

	@Test
	void contextLoads() {
	}

}
