package com.fanroute.sync.global.common.swagger;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.fanroute.sync.global.common.response.BaseCode;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(ApiErrorCodeExamples.List.class)
public @interface ApiErrorCodeExamples {

  Class<? extends BaseCode> type();

  String[] names();

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @interface List {

    ApiErrorCodeExamples[] value();
  }
}
