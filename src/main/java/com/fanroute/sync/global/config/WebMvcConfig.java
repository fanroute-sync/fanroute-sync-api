package com.fanroute.sync.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.http.converter.xml.JacksonXmlHttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** KOPIS용 XML 의존성이 API 응답 형식에 영향을 주지 않도록 XML converter를 제거합니다. */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

  @Override
  public void configureMessageConverters(HttpMessageConverters.ServerBuilder builder) {
    builder.configureMessageConvertersList(converters ->
        converters.removeIf(converter -> converter instanceof JacksonXmlHttpMessageConverter));
  }
}
