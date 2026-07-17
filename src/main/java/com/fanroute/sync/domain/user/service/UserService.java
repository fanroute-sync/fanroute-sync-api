package com.fanroute.sync.domain.user.service;

import java.time.Instant;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fanroute.sync.domain.user.entity.User;
import com.fanroute.sync.domain.user.entity.vo.AuthProvider;
import com.fanroute.sync.domain.user.entity.vo.UserStatus;
import com.fanroute.sync.domain.user.exception.UserErrorCode;
import com.fanroute.sync.domain.user.repository.UserRepository;
import com.fanroute.sync.global.common.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

  private static final int MAX_NICKNAME_GENERATION_ATTEMPTS = 10;

  private final UserRepository userRepository;
  private final NicknameGenerator nicknameGenerator;

  /** 신규 사용자를 생성합니다. */
  @Transactional
  public User createUser(AuthProvider authProvider, String providerUserId) {
    validateSocialAccountNotDuplicated(authProvider, providerUserId);

    String nickname = generateUniqueNickname();
    User user = User.create(nickname, authProvider, providerUserId);

    try {
      // 즉시 flush로 해당 트랜잭션에서 예외
      return userRepository.saveAndFlush(user);
    } catch (DataIntegrityViolationException e) {
      throw new BusinessException(UserErrorCode.USER_REGISTRATION_CONFLICT);
    }
  }

  /**
   * 사용자 ID로 활성 상태인 사용자를 조회합니다.
   */
  public User getAccessibleUser(Long userId) {
    User user = findUser(userId);
    validateAccessible(user);
    return user;
  }

  /**
   * 소셜 로그인 정보로 활성 상태인 사용자를 조회합니다.
   */
  public User getAccessibleUser(AuthProvider authProvider, String providerUserId) {
    User user = userRepository.findByAuthProviderAndProviderUserId(authProvider, providerUserId)
        .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
    validateAccessible(user);
    return user;
  }

  /**
   * 사용자 ID로 사용자를 조회합니다.
   * <p>
   * 상태와 무관하게 조회하므로 관리자 기능이나 내부 상태 변경 로직에서 사용합니다.
   * </p>
   */
  public User findUser(Long userId) {
    return userRepository.findById(userId)
        .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
  }

  /**
   * 사용자의 닉네임을 변경합니다.
   *
   * @throws BusinessException 닉네임이 중복되거나 변경할 수 없는 상태인 경우
   */
  @Transactional
  public User updateNickname(Long userId, String newNickname) {
    User user = findUser(userId);
    String normalized = newNickname == null ? null : newNickname.trim();

    if (normalized != null && normalized.equals(user.getNickname())) {
      return user;
    }

    if (userRepository.existsByNickname(normalized)) {
      throw new BusinessException(UserErrorCode.USER_DUPLICATE_NICKNAME);
    }

    try {
      user.updateNickname(newNickname);
      userRepository.flush();
    } catch (DataIntegrityViolationException e) {
      throw new BusinessException(UserErrorCode.USER_DUPLICATE_NICKNAME);
    }

    return user;
  }

  /** 사용자를 일시 정지 상태로 변경합니다. */
  @Transactional
  public void suspendUser(Long userId) {
    findUser(userId).suspend();
  }

  /** 정지된 사용자를 활성 상태로 변경합니다. */
  @Transactional
  public void activateUser(Long userId) {
    findUser(userId).activate();
  }

  /** 사용자를 탈퇴 처리합니다. */
  @Transactional
  public void withdrawUser(Long userId) {
    findUser(userId).withdraw(Instant.now());
  }

  private String generateUniqueNickname() {
    for (int attempt = 0; attempt < MAX_NICKNAME_GENERATION_ATTEMPTS; attempt++) {
      String nickname = nicknameGenerator.generate();
      if (!userRepository.existsByNickname(nickname)) {
        return nickname;
      }
    }
    throw new BusinessException(UserErrorCode.USER_NICKNAME_GENERATION_FAILED);
  }

  /**
   * @param authProvider   소셜 제공자
   * @param providerUserId 소셜 제공자가 발급한 고유 사용자 ID
   * @throws BusinessException 이미 가입된 소셜 계정이거나 고유한 닉네임 생성에 실패한 경우
   */
  private void validateSocialAccountNotDuplicated(AuthProvider authProvider, String providerUserId) {
    if (userRepository.existsByAuthProviderAndProviderUserId(authProvider, providerUserId)) {
      throw new BusinessException(UserErrorCode.USER_DUPLICATE_SOCIAL_ACCOUNT);
    }
  }

  /**
   * @param user User 엔티티
   * @throws BusinessException 정지 및 탈퇴 회원인 경우
   * 
   */
  private void validateAccessible(User user) {
    if (user.getStatus() == UserStatus.SUSPENDED) {
      throw new BusinessException(UserErrorCode.USER_SUSPENDED);
    }
    if (user.getStatus() == UserStatus.WITHDRAWN) {
      throw new BusinessException(UserErrorCode.USER_WITHDRAWN);
    }
  }
}