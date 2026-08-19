package com.fanroute.sync;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "auth.google.client-id=test-google-client",
    "auth.google.client-secret=test-google-secret",
    "auth.google.redirect-uri=http://localhost/test/callback",
    "auth.jwt.secret=dGVzdC1vbmx5LWtleS10aGF0LWlzLWF0LWxlYXN0LTMyLWJ5dGVzLWxvbmc=",
    "kopis.service-key=test-kopis-key",
    "admin.api-key=test-admin-key"
})
class FanrouteSyncApiApplicationTests {

	@Test
	void contextLoads() {
	}

}
