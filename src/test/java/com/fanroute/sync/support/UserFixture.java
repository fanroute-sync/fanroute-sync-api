package com.fanroute.sync.support;

import org.springframework.test.util.ReflectionTestUtils;

import com.fanroute.sync.domain.user.entity.User;
import com.fanroute.sync.domain.user.entity.vo.AuthProvider;

public final class UserFixture {

  private UserFixture() {
  }

  public static User activeUser() {
    return User.create("route", AuthProvider.GOOGLE, "google-1", "user@example.com");
  }

  public static User activeUserWithId(Long id) {
    User user = activeUser();
    ReflectionTestUtils.setField(user, "id", id);
    return user;
  }
}
