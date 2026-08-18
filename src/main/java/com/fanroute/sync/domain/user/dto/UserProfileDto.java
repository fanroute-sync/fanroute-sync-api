package com.fanroute.sync.domain.user.dto;

import com.fanroute.sync.domain.user.entity.User;
import com.fanroute.sync.domain.user.entity.vo.AuthProvider;
import com.fanroute.sync.domain.user.entity.vo.UserStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class UserProfileDto {

  public record Response(
      @Schema(description = "사용자 ID", example = "1") Long id,
      @Schema(description = "닉네임", example = "fanroute") String nickname,
      @Schema(description = "가입 이메일", example = "user@example.com") String email,
      @Schema(description = "소셜 로그인 제공자", example = "GOOGLE") AuthProvider authProvider,
      @Schema(description = "사용자 상태", example = "ACTIVE") UserStatus status) {

    public static Response from(User user) {
      return new Response(
          user.getId(),
          user.getNickname(),
          user.getEmail(),
          user.getAuthProvider(),
          user.getStatus());
    }
  }

  public record NicknameAvailabilityResponse(
      @Schema(description = "앞뒤 공백이 제거된 닉네임", example = "fanroute") String nickname,
      @Schema(description = "닉네임 사용 가능 여부", example = "true") boolean available) {

  }

  public record UpdateRequest(
      @Schema(description = "변경할 닉네임", example = "newNickname", requiredMode = Schema.RequiredMode.REQUIRED)
      String nickname) {

  }
}
