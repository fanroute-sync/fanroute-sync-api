package com.fanroute.sync.domain.schedule.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class CompanionTypeConverter implements AttributeConverter<CompanionType, String> {

  @Override
  public String convertToDatabaseColumn(CompanionType attribute) {
    return attribute == null ? null : attribute.getLabel();
  }

  @Override
  public CompanionType convertToEntityAttribute(String databaseData) {
    return databaseData == null ? null : CompanionType.from(databaseData);
  }
}
