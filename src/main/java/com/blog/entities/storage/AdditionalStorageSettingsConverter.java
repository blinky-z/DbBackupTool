package com.blog.entities.storage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.AttributeConverter;
import java.io.IOException;

class AdditionalStorageSettingsConverter implements AttributeConverter<AdditionalStorageSettings, String> {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final Logger logger = LoggerFactory.getLogger(AdditionalStorageSettingsConverter.class);

    private static final String ENTITY_TYPE = AdditionalStorageSettings.class.getName();
    private static final String DB_FIELD_TYPE = "JSON";

    @Override
    public String convertToDatabaseColumn(AdditionalStorageSettings attribute) {
        try {
            logger.trace("Serializing entity to JSON. From: {} - {}, To: {}", ENTITY_TYPE, attribute, DB_FIELD_TYPE);
            String attributeAsJson = objectMapper.writeValueAsString(attribute);
            logger.trace("Serialized entity: {}", attributeAsJson);
            return attributeAsJson;
        } catch (JsonProcessingException ex) {
            throw new RuntimeException(String.format("Error occurred while serializing entity. From: %s, To: %s",
                    ENTITY_TYPE, DB_FIELD_TYPE), ex);
        }
    }

    @Override
    public AdditionalStorageSettings convertToEntityAttribute(String dbData) {
        try {
            logger.trace("Deserializing JSON to entity. From: {} - {}, To: {}", DB_FIELD_TYPE, dbData, ENTITY_TYPE);
            AdditionalStorageSettings attribute = objectMapper.readValue(dbData, AdditionalStorageSettings.class);
            logger.trace("Deserialized entity: {}", attribute);
            return attribute;
        } catch (IOException ex) {
            throw new RuntimeException(String.format("Error occurred while deserializing database field. From: %s, To: %s",
                    DB_FIELD_TYPE, ENTITY_TYPE), ex);
        }
    }
}
