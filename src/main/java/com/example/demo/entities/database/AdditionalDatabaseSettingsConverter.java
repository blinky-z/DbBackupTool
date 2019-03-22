package com.example.demo.entities.database;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.persistence.AttributeConverter;
import java.io.IOException;

public class AdditionalDatabaseSettingsConverter implements AttributeConverter<AdditionalDatabaseSettings, String> {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final String ENTITY_TYPE = AdditionalDatabaseSettings.class.getName();
    private static final String DB_FIELD_TYPE = "JSON";

    @Override
    public String convertToDatabaseColumn(AdditionalDatabaseSettings attribute) {
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException ex) {
            throw new RuntimeException(String.format("Error occurred while serializing entity. From: %s, To: %s",
                    ENTITY_TYPE, DB_FIELD_TYPE), ex);
        }
    }

    @Override
    public AdditionalDatabaseSettings convertToEntityAttribute(String dbData) {
        try {
            return objectMapper.readValue(dbData, AdditionalDatabaseSettings.class);
        } catch (IOException ex) {
            throw new RuntimeException(String.format("Error occurred while deserializing database field. From: %s, To: %s",
                    DB_FIELD_TYPE, ENTITY_TYPE), ex);
        }
    }
}
