package com.fanroute.sync.domain.user.service;

public interface SessionRevoker {

  void revokeAll(Long userId);
}
