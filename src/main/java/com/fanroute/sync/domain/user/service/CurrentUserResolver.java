package com.fanroute.sync.domain.user.service;

import org.springframework.security.oauth2.jwt.Jwt;

import com.fanroute.sync.domain.user.entity.User;

public interface CurrentUserResolver {

  User getCurrentUser(Jwt jwt);
}
