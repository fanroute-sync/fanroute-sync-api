package com.fanroute.sync.global.config;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.method.HandlerMethod;

import com.fanroute.sync.global.common.response.BaseCode;
import com.fanroute.sync.global.common.response.ErrorCode;
import com.fanroute.sync.global.common.swagger.ApiErrorCodeExamples;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;

@OpenAPIDefinition(
    info = @Info(
        title = "FanRoute Sync API",
        description = "FanRoute Sync API specification",
        version = "v1"))
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT")
@Configuration
public class OpenApiConfig {

  @Bean
  public OperationCustomizer errorCodeExamplesCustomizer() {
    return (operation, handlerMethod) -> {
      addErrorCodeExamples(operation, handlerMethod);
      return operation;
    };
  }

  private void addErrorCodeExamples(Operation operation, HandlerMethod handlerMethod) {
    Method method = handlerMethod.getMethod();
    ApiErrorCodeExamples[] annotations =
        method.getAnnotationsByType(ApiErrorCodeExamples.class);

    if (annotations.length == 0) {
      return;
    }

    ApiResponses responses = operation.getResponses();
    if (responses == null) {
      responses = new ApiResponses();
      operation.setResponses(responses);
    }

    for (ApiErrorCodeExamples annotation : annotations) {
      Map<String, BaseCode> constants = enumConstants(annotation.type());
      for (String name : annotation.names()) {
        BaseCode errorCode = constants.get(name);
        if (errorCode == null) {
          throw new IllegalArgumentException(
              annotation.type().getSimpleName() + "에 " + name + " 상수가 없습니다.");
        }
        addErrorResponse(responses, errorCode);
      }
    }
  }

  private Map<String, BaseCode> enumConstants(Class<? extends BaseCode> type) {
    if (!type.isEnum()) {
      throw new IllegalArgumentException(type.getName() + "은 Enum 타입이어야 합니다.");
    }

    Map<String, BaseCode> constants = new LinkedHashMap<>();
    Arrays.stream(type.getEnumConstants())
        .forEach(constant -> constants.put(((Enum<?>) constant).name(), constant));
    return constants;
  }

  private void addErrorResponse(ApiResponses responses, BaseCode errorCode) {
    String responseCode = String.valueOf(errorCode.getHttpStatus().value());
    ApiResponse response = responses.computeIfAbsent(
        responseCode,
        key -> new ApiResponse().description(errorCode.getHttpStatus().getReasonPhrase()));

    io.swagger.v3.oas.models.media.MediaType mediaType = response.getContent() == null
        ? new io.swagger.v3.oas.models.media.MediaType()
        : response.getContent().get(MediaType.APPLICATION_JSON_VALUE);
    if (mediaType == null) {
      mediaType = new io.swagger.v3.oas.models.media.MediaType();
    }
    mediaType.setSchema(errorResponseSchema());
    mediaType.addExamples(errorCode.getCode(), new Example().value(errorResponseExample(errorCode)));

    if (response.getContent() == null) {
      response.setContent(new io.swagger.v3.oas.models.media.Content());
    }
    response.getContent().addMediaType(MediaType.APPLICATION_JSON_VALUE, mediaType);
  }

  private ObjectSchema errorResponseSchema() {
    ObjectSchema schema = new ObjectSchema();
    schema.addProperty("success", new BooleanSchema().example(false));
    schema.addProperty("status", new IntegerSchema().format("int32"));
    schema.addProperty("code", new StringSchema());
    schema.addProperty("message", new StringSchema());
    schema.addProperty("data", new io.swagger.v3.oas.models.media.Schema<>().nullable(true));
    schema.setRequired(java.util.List.of("success", "status", "code", "message"));
    return schema;
  }

  private Map<String, Object> errorResponseExample(BaseCode errorCode) {
    Map<String, Object> example = new LinkedHashMap<>();
    example.put("success", false);
    example.put("status", errorCode.getHttpStatus().value());
    example.put("code", errorCode.getCode());
    example.put("message", errorCode.getMessage());
    if (errorCode == ErrorCode.INVALID_PARAMETER) {
      example.put("data", java.util.List.of(Map.of(
          "field", "requestField",
          "message", "입력값이 올바르지 않습니다.")));
    }
    return example;
  }
}
