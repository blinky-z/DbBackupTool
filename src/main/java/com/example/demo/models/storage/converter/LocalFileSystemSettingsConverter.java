package com.example.demo.models.storage.converter;

import com.example.demo.models.storage.LocalFileSystemSettings;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.io.IOException;

@Converter
public class LocalFileSystemSettingsConverter implements AttributeConverter<LocalFileSystemSettings, String> {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final String DB_FIELD_TYPE = "JSON String";
    private static final String ENTITY_TYPE = "Local File System Settings";

    @Override
    public String convertToDatabaseColumn(LocalFileSystemSettings attribute) {
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException ex) {
            throw new RuntimeException("Error occurred while serializing " + ENTITY_TYPE + " Entity to " + DB_FIELD_TYPE, ex);
        }
    }

    @Override
    public LocalFileSystemSettings convertToEntityAttribute(String dbData) {
        try {
            return objectMapper.readValue(dbData, LocalFileSystemSettings.class);
        } catch (IOException ex) {
            throw new RuntimeException("Error occurred while deserializing " + DB_FIELD_TYPE + " to " + ENTITY_TYPE + " Entity", ex);
        }
    }
}
