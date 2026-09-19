package com.fanroute.sync.global.config;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.method.HandlerMethod;

import com.fanroute.sync.domain.auth.exception.AuthErrorCode;
import com.fanroute.sync.global.common.response.BaseCode;
import com.fanroute.sync.global.common.response.ErrorCode;
import com.fanroute.sync.global.common.swagger.ApiErrorCodeExamples;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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

  private static final String BEARER_AUTH_SECURITY_SCHEME = "bearerAuth";

  @Bean
  public OperationCustomizer errorCodeExamplesCustomizer() {
    return (operation, handlerMethod) -> {
      addErrorCodeExamples(operation, handlerMethod);
      return operation;
    };
  }

  private void addErrorCodeExamples(Operation operation, HandlerMethod handlerMethod) {
    ApiErrorCodeExamples[] annotations = apiMethods(handlerMethod).stream()
        .flatMap(method -> Arrays.stream(method.getAnnotationsByType(ApiErrorCodeExamples.class)))
        .toArray(ApiErrorCodeExamples[]::new);
    boolean requiresBearerAuth = requiresBearerAuth(handlerMethod);

    if (annotations.length == 0 && !requiresBearerAuth) {
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

    // bearerAuth로 보호된 엔드포인트는 도메인 컨트롤러가 AuthErrorCode를 직접 참조하지 않아도
    // Access Token 무효 응답 예시를 공통으로 문서화합니다.
    if (requiresBearerAuth) {
      addErrorResponse(responses, AuthErrorCode.ACCESS_TOKEN_INVALID);
    }
  }

  private boolean requiresBearerAuth(HandlerMethod handlerMethod) {
    return hasBearerAuthRequirement(handlerMethod.getMethod())
        || hasBearerAuthRequirement(handlerMethod.getBeanType())
        || apiMethods(handlerMethod).stream()
            .anyMatch(method -> hasBearerAuthRequirement(method.getDeclaringClass()));
  }

  private List<Method> apiMethods(HandlerMethod handlerMethod) {
    List<Method> methods = new ArrayList<>();
    Method handler = handlerMethod.getMethod();
    methods.add(handler);
    addInterfaceMethods(handlerMethod.getBeanType(), handler, methods);
    return methods;
  }

  private void addInterfaceMethods(Class<?> type, Method handler, List<Method> methods) {
    for (Class<?> api : type.getInterfaces()) {
      try {
        methods.add(api.getMethod(handler.getName(), handler.getParameterTypes()));
      } catch (NoSuchMethodException exception) {
        // 해당 인터페이스가 현재 핸들러 메서드를 선언하지 않은 경우 무시합니다.
      }
      addInterfaceMethods(api, handler, methods);
    }
  }

  private boolean hasBearerAuthRequirement(AnnotatedElement element) {
    return Arrays.stream(element.getAnnotationsByType(SecurityRequirement.class))
        .anyMatch(requirement -> BEARER_AUTH_SECURITY_SCHEME.equals(requirement.name()));
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
