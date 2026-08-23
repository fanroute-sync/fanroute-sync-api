package com.fanroute.sync.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.web.method.HandlerMethod;

import com.fanroute.sync.domain.schedule.controller.ItineraryController;
import com.fanroute.sync.domain.schedule.controller.AiItineraryGenerationController;
import com.fanroute.sync.domain.schedule.dto.ItineraryDto;
import com.fanroute.sync.global.common.response.ErrorCode;
import com.fanroute.sync.global.common.swagger.ApiErrorCodeExamples;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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

  @Test
  @DisplayName("bearerAuth 보안 요구사항이 있는 엔드포인트에 Access Token 무효 응답을 자동으로 추가한다")
  void addsAccessTokenInvalidResponseForBearerAuthEndpoint() throws NoSuchMethodException {
    OpenApiConfig config = new OpenApiConfig();
    OperationCustomizer customizer = config.errorCodeExamplesCustomizer();
    BearerAuthController controller = new BearerAuthController();
    Method method = BearerAuthController.class.getMethod("endpoint");
    Operation operation = new Operation().responses(
        new ApiResponses().addApiResponse("200", new ApiResponse().description("성공")));

    customizer.customize(operation, new HandlerMethod(controller, method));

    ApiResponse unauthorized = operation.getResponses().get("401");
    assertThat(unauthorized).isNotNull();
    assertThat(unauthorized.getContent().get("application/json").getExamples())
        .containsKey("AUTH_ACCESS_TOKEN_INVALID");
  }

  @Test
  @DisplayName("bearerAuth 보안 요구사항이 없는 엔드포인트에는 Access Token 무효 응답을 추가하지 않는다")
  void doesNotAddAccessTokenInvalidResponseWithoutBearerAuth() throws NoSuchMethodException {
    OpenApiConfig config = new OpenApiConfig();
    OperationCustomizer customizer = config.errorCodeExamplesCustomizer();
    TestController controller = new TestController();
    Method method = TestController.class.getMethod("endpoint");
    Operation operation = new Operation().responses(
        new ApiResponses().addApiResponse("200", new ApiResponse().description("성공")));

    customizer.customize(operation, new HandlerMethod(controller, method));

    assertThat(operation.getResponses().get("401")).isNull();
  }

  @Test
  @DisplayName("일정 항목 수정 API의 Swagger 오류 응답을 생성한다")
  void addsItineraryUpdateErrorResponses() throws NoSuchMethodException {
    OpenApiConfig config = new OpenApiConfig();
    OperationCustomizer customizer = config.errorCodeExamplesCustomizer();
    ItineraryController controller = new ItineraryController(null, null);
    Method method = ItineraryController.class.getMethod("updateItem",
        org.springframework.security.oauth2.jwt.Jwt.class, Long.class,
        ItineraryDto.UpdateItemRequest.class);
    Operation operation = new Operation().responses(
        new ApiResponses().addApiResponse("200", new ApiResponse().description("성공")));

    customizer.customize(operation, new HandlerMethod(controller, method));

    assertThat(operation.getResponses()).containsKeys("200", "400", "401", "404");
    assertThat(operation.getResponses().get("400").getContent().get("application/json").getExamples())
        .containsKeys("SCHEDULE_INVALID_ITINERARY_ITEM", "SCHEDULE_FIXED_ITINERARY_ITEM");
    assertThat(operation.getResponses().get("404").getContent().get("application/json").getExamples())
        .containsKey("SCHEDULE_ITINERARY_ITEM_NOT_FOUND");
  }

  @Test
  @DisplayName("AI 일정 생성 요청 API의 Swagger 오류 응답을 생성한다")
  void addsAiGenerationRequestErrorResponses() throws NoSuchMethodException {
    OpenApiConfig config = new OpenApiConfig();
    OperationCustomizer customizer = config.errorCodeExamplesCustomizer();
    AiItineraryGenerationController controller = new AiItineraryGenerationController(null, null);
    Method method = AiItineraryGenerationController.class.getMethod("requestGeneration",
        org.springframework.security.oauth2.jwt.Jwt.class, Long.class);
    Operation operation = new Operation().responses(
        new ApiResponses().addApiResponse("202", new ApiResponse().description("접수")));

    customizer.customize(operation, new HandlerMethod(controller, method));

    assertThat(operation.getResponses()).containsKeys("202", "401", "404");
    assertThat(operation.getResponses().get("404").getContent().get("application/json").getExamples())
        .containsKey("SCHEDULE_ITINERARY_DAY_NOT_FOUND");
  }

  @Test
  @DisplayName("AI 일정 생성 재시도 API의 Swagger 오류 응답을 생성한다")
  void addsAiGenerationRetryErrorResponses() throws NoSuchMethodException {
    OpenApiConfig config = new OpenApiConfig();
    OperationCustomizer customizer = config.errorCodeExamplesCustomizer();
    AiItineraryGenerationController controller = new AiItineraryGenerationController(null, null);
    Method method = AiItineraryGenerationController.class.getMethod("retryGeneration",
        org.springframework.security.oauth2.jwt.Jwt.class, Long.class);
    Operation operation = new Operation().responses(
        new ApiResponses().addApiResponse("202", new ApiResponse().description("접수")));

    customizer.customize(operation, new HandlerMethod(controller, method));

    assertThat(operation.getResponses()).containsKeys("202", "400", "401", "404");
    assertThat(operation.getResponses().get("400").getContent().get("application/json").getExamples())
        .containsKey("SCHEDULE_INVALID_AI_ITINERARY_GENERATION_STATUS");
    assertThat(operation.getResponses().get("404").getContent().get("application/json").getExamples())
        .containsKey("SCHEDULE_AI_ITINERARY_GENERATION_NOT_FOUND");
  }

  @Test
  @DisplayName("AI 일정 생성 취소 API의 Swagger 오류 응답을 생성한다")
  void addsAiGenerationCancelErrorResponses() throws NoSuchMethodException {
    OpenApiConfig config = new OpenApiConfig();
    OperationCustomizer customizer = config.errorCodeExamplesCustomizer();
    AiItineraryGenerationController controller = new AiItineraryGenerationController(null, null);
    Method method = AiItineraryGenerationController.class.getMethod("cancelGeneration",
        org.springframework.security.oauth2.jwt.Jwt.class, Long.class);
    Operation operation = new Operation().responses(
        new ApiResponses().addApiResponse("200", new ApiResponse().description("성공")));

    customizer.customize(operation, new HandlerMethod(controller, method));

    assertThat(operation.getResponses()).containsKeys("200", "400", "401", "404");
    assertThat(operation.getResponses().get("400").getContent().get("application/json").getExamples())
        .containsKey("SCHEDULE_INVALID_AI_ITINERARY_GENERATION_STATUS");
    assertThat(operation.getResponses().get("404").getContent().get("application/json").getExamples())
        .containsKey("SCHEDULE_AI_ITINERARY_GENERATION_NOT_FOUND");
  }

  private static class TestController {

    @ApiErrorCodeExamples(type = ErrorCode.class, names = "INVALID_PARAMETER")
    public void endpoint() {

    }
  }

  @SecurityRequirement(name = "bearerAuth")
  private static class BearerAuthController {

    public void endpoint() {

    }
  }
}
