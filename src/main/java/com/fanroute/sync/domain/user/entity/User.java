package com.fanroute.sync.domain.user.entity;

import static com.fanroute.sync.global.util.TextValidator.normalizeRequired;
import static com.fanroute.sync.global.util.TextValidator.requireNonNull;

import java.time.Instant;
import java.util.Objects;

import com.fanroute.sync.domain.user.entity.vo.AuthProvider;
import com.fanroute.sync.domain.user.entity.vo.UserStatus;
import com.fanroute.sync.domain.user.exception.UserErrorCode;
import com.fanroute.sync.global.common.entity.SoftDeleteEntity;
import com.fanroute.sync.global.common.exception.BusinessException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "users", uniqueConstraints = {
    // 소셜 계정 하나가 여러 사용자와 연결되는 것을 방지
    @UniqueConstraint(name = "uk_users_auth_provider_provider_user_id", columnNames = {
        "auth_provider",
        "provider_user_id"}),
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends SoftDeleteEntity {

  private static final int MAX_NICKNAME_LENGTH = 30;
  private static final int MAX_PROVIDER_USER_ID_LENGTH = 255;
  private static final int MAX_EMAIL_LENGTH = 320;
  private static final String WITHDRAWN_NICKNAME_PREFIX = "withdrawn_";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = MAX_NICKNAME_LENGTH)
  private String nickname;

  @Enumerated(EnumType.STRING)
  @Column(name = "auth_provider", nullable = false, length = 20)
  private AuthProvider authProvider; // 소셜 로그인 제공자 (GOOGLE, APPLE 등)

  @Column(name = "provider_user_id", nullable = false, length = MAX_PROVIDER_USER_ID_LENGTH)
  private String providerUserId; // 소셜 제공자가 발급한 고유 사용자 ID

  @Column(length = MAX_EMAIL_LENGTH)
  private String email;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private UserStatus status; // 회원의 현재 이용 상태

  private User(String nickname, AuthProvider authProvider, String providerUserId, String email) {
    this.nickname = normalizeNickname(nickname);

    this.authProvider = requireNonNull(authProvider, UserErrorCode.USER_INVALID_AUTH_PROVIDER);

    this.providerUserId = normalizeRequired(providerUserId, MAX_PROVIDER_USER_ID_LENGTH,
        UserErrorCode.USER_INVALID_PROVIDER_USER_ID);

    this.email = email;

    this.status = UserStatus.ACTIVE;
  }

  /**
   * 신규 사용자 생성
   * <p>
   * 닉네임은 반드시 서비스 계층에서 {@code NicknameGenerator}로 생성해야 함 엔티티 계층에서 생성 시, 테스트 및 중복시 재시도가 어려움
   * </p>
   */
  public static User create(String nickname, AuthProvider authProvider, String providerUserId) {
    return new User(nickname, authProvider, providerUserId, null);
  }

  public static User create(
      String nickname, AuthProvider authProvider, String providerUserId, String email) {
    return new User(nickname, authProvider, providerUserId, email);
  }

  /**
   * 사용자의 닉네임 변경
   *
   * @param newNickname 변경할 닉네임
   * @throws BusinessException 탈퇴한 사용자이거나 닉네임이 유효하지 않은 경우
   */
  public void updateNickname(String newNickname) {
    ensureNotWithdrawn(UserErrorCode.USER_WITHDRAWN);
    this.nickname = normalizeNickname(newNickname);
  }

  /**
   * 회원을 일시 정지(제재) 상태로 변경합니다.
   *
   * @throws BusinessException 탈퇴한 회원인 경우 발생
   */
  public void suspend() {
    ensureNotWithdrawn(UserErrorCode.USER_WITHDRAWN);
    this.status = UserStatus.SUSPENDED;
  }

  /**
   * 회원을 일반 활성 상태로 변경(또는 제재 해제)
   *
   * @throws BusinessException 탈퇴한 회원인 경우 발생
   */
  public void activate() {
    ensureNotWithdrawn(UserErrorCode.USER_WITHDRAWN);
    this.status = UserStatus.ACTIVE;
  }

  /**
   * 회원을 탈퇴 상태로 변경하고 논리 삭제 시각을 기록합니다.
   * <p>
   * 기존 닉네임은 {@code withdrawn_{userId}} 형식의 tombstone 값으로 변경하여 다른 사용자가 기존 닉네임을 다시 사용할 수 있게 합니다.
   * </p>
   *
   * @param withdrawnAt 탈퇴 처리 시각
   * @throws BusinessException     이미 탈퇴한 사용자인 경우
   * @throws IllegalStateException 아직 DB에 영속화(저장)되지 않아 ID가 없는 유저인 경우
   * @throws NullPointerException  탈퇴 처리 시각이 null인 경우
   */
  public void withdraw(Instant withdrawnAt) {
    ensureNotWithdrawn(UserErrorCode.USER_ALREADY_WITHDRAWN);

    // NOTE:: 아래의 2개의 예외 모두 Developer Exception이므로 해당으로 처리
    if (this.id == null) {
      throw new IllegalStateException("Cannot withdraw a transient user entity that has no ID");
    }
    Instant deletedAt = Objects.requireNonNull(withdrawnAt, "withdrawnAt must not be null");

    this.status = UserStatus.WITHDRAWN;
    markDeleted(deletedAt);
    this.nickname = WITHDRAWN_NICKNAME_PREFIX + id;
  }

  /**
   * 일반 사용자가 사용할 닉네임을 검증합니다.
   * <p>
   * 시스템 tombstone 값과 충돌하지 않도록 {@code withdrawn_}로 시작하는 닉네임은 허용하지 않습니다.
   * </p>
   */
  public static String normalizeNickname(String nickname) {
    String normalized = normalizeRequired(nickname, MAX_NICKNAME_LENGTH,
        UserErrorCode.USER_INVALID_NICKNAME);
    if (normalized.startsWith(WITHDRAWN_NICKNAME_PREFIX)) {
      throw new BusinessException(UserErrorCode.USER_INVALID_NICKNAME);
    }
    return normalized;
  }

  private void ensureNotWithdrawn(UserErrorCode errorCode) {
    if (status == UserStatus.WITHDRAWN) {
      throw new BusinessException(errorCode);
    }
  }
}