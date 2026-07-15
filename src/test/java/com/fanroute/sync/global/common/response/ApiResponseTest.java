package com.fanroute.sync.global.common.response;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class ApiResponseTest {

  @Test
  @DisplayName("ok는 200 성공 응답을 생성한다")
  void ok() {
    ApiResponse<String> response = ApiResponse.ok("data");

    assertThat(response.success()).isTrue();
    assertThat(response.status()).isEqualTo(200);
    assertThat(response.message())
        .isEqualTo("요청이 성공적으로 처리되었습니다.");
    assertThat(response.data()).isEqualTo("data");
  }

  @Test
  @DisplayName("fail은 ErrorCode에 맞는 실패 응답을 생성한다")
  void fail() {
    ApiResponse<Void> response =
        ApiResponse.fail(ErrorCode.DATA_NOT_FOUND);

    assertThat(response.success()).isFalse();
    assertThat(response.status()).isEqualTo(404);
    assertThat(response.code()).isEqualTo("D001");
    assertThat(response.message())
        .isEqualTo("요청한 데이터를 찾을 수 없습니다.");
    assertThat(response.data()).isNull();
  }

  @Test
  @DisplayName("toResponseEntity는 응답 status를 HTTP 상태로 사용한다")
  void toResponseEntity() {
    ApiResponse<Void> response =
        ApiResponse.fail(ErrorCode.INVALID_PARAMETER);

    ResponseEntity<ApiResponse<Void>> entity =
        response.toResponseEntity();

    assertThat(entity.getStatusCode().value()).isEqualTo(400);
    assertThat(entity.getBody()).isEqualTo(response);
  }
}