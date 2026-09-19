package com.fanroute.sync.domain.schedule.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class TravelMbtiTypeConverter implements AttributeConverter<TravelMbtiType, String> {

  @Override
  public String convertToDatabaseColumn(TravelMbtiType attribute) {
    return attribute == null ? null : attribute.getLabel();
  }

  @Override
  public TravelMbtiType convertToEntityAttribute(String databaseData) {
    return databaseData == null ? null : TravelMbtiType.from(databaseData);
  }
}
