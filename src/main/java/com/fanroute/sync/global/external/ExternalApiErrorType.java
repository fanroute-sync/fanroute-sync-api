package com.fanroute.sync.global.external;

public enum ExternalApiErrorType {
  CLIENT_ERROR, // 4xx
  SERVER_ERROR, // 5xx
  CONNECT_TIMEOUT, // 연결 과정에서 timeout
  READ_TIMEOUT, // 응답 과정에서 timeout
  CONNECTION_ERROR // 연결 과정에서 에러(연결 거부, DNS 등)
}