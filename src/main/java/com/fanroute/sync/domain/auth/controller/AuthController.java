package com.fanroute.sync.domain.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fanroute.sync.domain.auth.dto.LoginDto;
import com.fanroute.sync.domain.auth.service.GoogleLoginService;
import com.fanroute.sync.global.common.response.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 클라이언트가 전달한 Google 인가 코드를 처리하는 공개 인증 API입니다.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

  private final GoogleLoginService googleLoginService;

  /**
   * Google 로그인 성공 시 서비스 API에서 사용할 JWT Access Token을 반환합니다.
   */
  @PostMapping("/google")
  public ResponseEntity<ApiResponse<LoginDto.Response>> googleLogin(
      @Valid @RequestBody LoginDto.Request request) {
    return ApiResponse.ok(googleLoginService.login(request.authorizationCode())).toResponseEntity();
  }

  /**
   * TODO:: 프론트 완성 후 삭제
   * 프론트엔드가 없는 로컬 환경에서 Google 리디렉션을 직접 처리하기 위한 콜백입니다.
   */
  @GetMapping("/google/callback")
  public ResponseEntity<ApiResponse<LoginDto.Response>> googleCallback(
      @RequestParam("code") String authorizationCode,
      @RequestParam(value = "state", required = false) String state
  ) {
    LoginDto.Response response =
        googleLoginService.login(authorizationCode);

    return ApiResponse.ok(response).toResponseEntity();
  }
}