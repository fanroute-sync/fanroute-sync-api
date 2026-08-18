package com.fanroute.sync.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.web.method.HandlerMethod;

import com.fanroute.sync.global.common.response.ErrorCode;
import com.fanroute.sync.global.common.swagger.ApiErrorCodeExamples;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;

class OpenApiConfigTest {

  @Test
  @DisplayName("오류 코드 어노테이션을 OpenAPI 오류 응답으로 변환한다")
  void addsErrorResponseFromErrorCodeAnnotation() throws NoSuchMethodException {
    OpenApiConfig config = new OpenApiConfig();
    OperationCustomizer customizer = config.errorCodeExamplesCustomizer();
    TestController controller = new TestController();
    Method method = TestController.class.getMethod("endpoint");
    Operation operation = new Operation().responses(
        new ApiResponses().addApiResponse("200", new ApiResponse().description("성공")));

    customizer.customize(operation, new HandlerMethod(controller, method));

    ApiResponse badRequest = operation.getResponses().get("400");
    assertThat(badRequest.getContent().get("application/json").getSchema()).isNotNull();
    assertThat(badRequest.getContent().get("application/json").getExamples())
        .containsKey("COMMON_INVALID_PARAMETER");
  }

  private static class TestController {

    @ApiErrorCodeExamples(type = ErrorCode.class, names = "INVALID_PARAMETER")
    public void endpoint() {

    }
  }
}
