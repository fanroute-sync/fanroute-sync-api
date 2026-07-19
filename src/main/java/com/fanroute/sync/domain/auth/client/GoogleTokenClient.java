package com.fanroute.sync.domain.auth.client;

import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.PostExchange;

import com.fanroute.sync.domain.auth.dto.GoogleOAuthDto;

/**
 * Google 인가 코드를 Token Endpoint에 전달하는 HTTP Interface입니다.
 */
public interface GoogleTokenClient {

  @PostExchange(url = "/token", contentType = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  GoogleOAuthDto.TokenResponse exchangeToken(@RequestBody MultiValueMap<String, String> form);
}