package com.fanroute.sync.global.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.http.HttpConnectTimeoutException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.service.annotation.GetExchange;

import com.fanroute.sync.global.external.ExternalApiClientFactory;
import com.fanroute.sync.global.external.ExternalApiErrorType;
import com.fanroute.sync.global.external.ExternalApiException;
import com.fanroute.sync.global.external.ExternalApiLoggingInterceptor;
import com.fanroute.sync.global.external.ExternalApiTransportExceptionMapper;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;

class ExternalApiClientConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(ExternalApiClientConfig.class)
            .withPropertyValues(
                    "external-api.http.connect-timeout=1s",
                    "external-api.http.read-timeout=2s",
                    "external-api.services.test.base-url=https://example.com");

    @Test
    @DisplayName("외부 API 공통 빈과 설정을 컨텍스트에 등록한다")
    void loadsCommonClientConfiguration() {
        contextRunner.run(context -> {
            assertThat(context).hasBean("externalApiRestClientBuilder");
            assertThat(context).hasSingleBean(RestClient.Builder.class);
            assertThat(context).hasSingleBean(ExternalApiLoggingInterceptor.class);
            assertThat(context).hasSingleBean(ExternalApiClientFactory.class);
            assertThat(context.getBean(ExternalApiProperties.class)
                    .getRequiredService("test").getBaseUrl())
                    .isEqualTo(URI.create("https://example.com"));
        });
    }

    @Test
    @DisplayName("외부 API Base URL이 없으면 프로퍼티 검증에 실패한다")
    void rejectsMissingServiceBaseUrl() {
        ExternalApiProperties properties = new ExternalApiProperties();
        properties.getServices().put("test", new ExternalApiProperties.Service());

        try (ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory()) {
            assertThat(validatorFactory.getValidator().validate(properties))
                    .anyMatch(violation -> violation.getPropertyPath().toString().endsWith("baseUrl"));
        }
    }

    @Test
    @DisplayName("HTTP 인터페이스 클라이언트로 정상 응답을 조회한다")
    void createsHttpInterfaceClientAndHandlesSuccessfulResponse() {
        contextRunner.run(context -> {
            RestClient.Builder builder = context.getBean(RestClient.Builder.class);
            MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
            server.expect(requestTo("https://example.com/message"))
                    .andRespond(withSuccess("ok", MediaType.TEXT_PLAIN));

            TestClient client = createTestClient(context);

            assertThat(client.getMessage()).isEqualTo("ok");
            server.verify();
        });
    }

    @Test
    @DisplayName("4xx 응답을 클라이언트 오류 예외로 변환한다")
    void converts4xxResponseToCommonException() {
        assertStatusError(HttpStatus.BAD_REQUEST, ExternalApiErrorType.CLIENT_ERROR);
    }

    @Test
    @DisplayName("5xx 응답을 서버 오류 예외로 변환한다")
    void converts5xxResponseToCommonException() {
        assertStatusError(HttpStatus.SERVICE_UNAVAILABLE, ExternalApiErrorType.SERVER_ERROR);
    }

    @Test
    @DisplayName("읽기 타임아웃을 공통 예외로 변환하고 내부 메시지를 노출하지 않는다")
    void convertsReadTimeoutDuringRequestToCommonException() {
        MockClientHttpRequest request = new MockClientHttpRequest(
                HttpMethod.GET, URI.create("https://example.com/message?apiKey=secret"));
        ExternalApiLoggingInterceptor interceptor = new ExternalApiLoggingInterceptor();

        assertThatThrownBy(() -> interceptor.intercept(
                request,
                new byte[0],
                (ignoredRequest, ignoredBody) -> {
                    throw new SocketTimeoutException("sensitive low-level message");
                }))
                .isInstanceOfSatisfying(
                        ExternalApiException.class,
                        exception -> {
                            assertThat(exception.getErrorType())
                                    .isEqualTo(ExternalApiErrorType.READ_TIMEOUT);
                            assertThat(exception.getStatusCode()).isNull();
                            assertThat(exception.getCause())
                                    .isInstanceOf(SocketTimeoutException.class);
                            assertThat(exception.getMessage()).doesNotContain("sensitive");
                        });
    }

    @Test
    @DisplayName("연결 타임아웃을 공통 예외로 변환한다")
    void convertsConnectTimeoutToCommonException() {
        ExternalApiException exception = ExternalApiTransportExceptionMapper.convert(
                new HttpConnectTimeoutException("connection timed out"));

        assertThat(exception.getErrorType()).isEqualTo(ExternalApiErrorType.CONNECT_TIMEOUT);
        assertThat(exception.getStatusCode()).isNull();
    }

    @Test
    @DisplayName("연결 실패를 공통 예외로 변환한다")
    void convertsConnectionFailureToCommonException() {
        ExternalApiException exception = ExternalApiTransportExceptionMapper.convert(
                new ConnectException("connection refused"));

        assertThat(exception.getErrorType()).isEqualTo(ExternalApiErrorType.CONNECTION_ERROR);
        assertThat(exception.getStatusCode()).isNull();
    }

    private void assertStatusError(HttpStatus status, ExternalApiErrorType expectedType) {
        contextRunner.run(context -> {
            RestClient.Builder builder = context.getBean(RestClient.Builder.class);
            MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
            server.expect(requestTo("https://example.com/message")).andRespond(withStatus(status));

            TestClient client = createTestClient(context);

            assertThatThrownBy(client::getMessage)
                    .isInstanceOfSatisfying(
                            ExternalApiException.class,
                            exception -> {
                                assertThat(exception.getErrorType()).isEqualTo(expectedType);
                                assertThat(exception.getStatusCode().value()).isEqualTo(status.value());
                            });
            server.verify();
        });
    }

    private TestClient createTestClient(ApplicationContext context) {
        URI baseUrl = context.getBean(ExternalApiProperties.class)
                .getRequiredService("test")
                .getBaseUrl();
        return context.getBean(ExternalApiClientFactory.class)
                .createClient(TestClient.class, baseUrl);
    }

    interface TestClient {
        @GetExchange("/message")
        String getMessage();
    }
}
