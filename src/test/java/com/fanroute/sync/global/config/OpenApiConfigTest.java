package com.fanroute.sync.global.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Method;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.web.method.HandlerMethod;

import com.fanroute.sync.domain.auth.controller.AuthController;
import com.fanroute.sync.domain.auth.dto.LoginDto;
import com.fanroute.sync.domain.auth.service.GoogleLoginService;
import com.fanroute.sync.domain.auth.service.TokenRefreshService;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;

class OpenApiConfigTest {

  @Test
  void addsErrorResponseSchemaAndExamplesFromErrorCodes() throws NoSuchMethodException {
    OpenApiConfig config = new OpenApiConfig();
    OperationCustomizer customizer = config.errorCodeExamplesCustomizer();
    AuthController controller = new AuthController(
        mock(GoogleLoginService.class), mock(TokenRefreshService.class));
    Method method = AuthController.class.getMethod("googleLogin", LoginDto.Request.class);
    Operation operation = new Operation().responses(
        new ApiResponses().addApiResponse("200", new ApiResponse().description("로그인 성공")));

    customizer.customize(operation, new HandlerMethod(controller, method));

    ApiResponse badRequest = operation.getResponses().get("400");
    ApiResponse unauthorized = operation.getResponses().get("401");
    ApiResponse badGateway = operation.getResponses().get("502");

    assertThat(badRequest.getContent().get("application/json").getSchema()).isNotNull();
    assertThat(badRequest.getContent().get("application/json").getExamples())
        .containsKey("COMMON_INVALID_PARAMETER");
    assertThat(unauthorized.getContent().get("application/json").getExamples())
        .containsKeys("AUTH_GOOGLE_CODE_INVALID", "AUTH_GOOGLE_ID_TOKEN_INVALID");
    assertThat(badGateway.getContent().get("application/json").getExamples())
        .containsKey("AUTH_GOOGLE_API_UNAVAILABLE");

    @SuppressWarnings("unchecked")
    Map<String, Object> example = (Map<String, Object>) unauthorized.getContent()
        .get("application/json")
        .getExamples()
        .get("AUTH_GOOGLE_CODE_INVALID")
        .getValue();
    assertThat(example)
        .containsEntry("success", false)
        .containsEntry("status", 401)
        .containsEntry("code", "AUTH_GOOGLE_CODE_INVALID")
        .containsEntry("message", "Google 인가 코드가 유효하지 않습니다.");

    @SuppressWarnings("unchecked")
    Map<String, Object> validationExample = (Map<String, Object>) badRequest.getContent()
        .get("application/json")
        .getExamples()
        .get("COMMON_INVALID_PARAMETER")
        .getValue();
    assertThat(validationExample.get("data")).isInstanceOf(java.util.List.class);
  }
}
