package com.fanroute.sync.domain.concert.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** 기존 genre_name 데이터와 호환되도록 Enum 상수명 대신 KOPIS 장르명을 저장합니다. */
@Converter(autoApply = true)
public class GenreConverter implements AttributeConverter<Genre, String> {

  @Override
  public String convertToDatabaseColumn(Genre genre) {
    return genre == null ? null : genre.label();
  }

  @Override
  public Genre convertToEntityAttribute(String dbLabel) {
    return dbLabel == null ? null : Genre.fromLabel(dbLabel);
  }
}
