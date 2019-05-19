package com.blog.entities.database;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.AttributeConverter;
import java.io.IOException;

class AdditionalDatabaseSettingsConverter implements AttributeConverter<AdditionalDatabaseSettings, String> {
    private static final ObjectMapper objectMapper = new ObjectMapper().disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

    private static final Logger logger = LoggerFactory.getLogger(AdditionalDatabaseSettingsConverter.class);

    private static final String ENTITY_TYPE = AdditionalDatabaseSettings.class.getName();
    private static final String DB_FIELD_TYPE = "JSON";

    @Override
    public String convertToDatabaseColumn(AdditionalDatabaseSettings attribute) {
        try {
            logger.debug("Serializing entity to JSON. From: {} - {}, To: {}", ENTITY_TYPE, attribute, DB_FIELD_TYPE);
            String attributeAsJson = objectMapper.writeValueAsString(attribute);
            logger.debug("Serialized entity: {}", attributeAsJson);
            return attributeAsJson;
        } catch (JsonProcessingException ex) {
            throw new RuntimeException(String.format("Error occurred while serializing entity. From: %s, To: %s",
                    ENTITY_TYPE, DB_FIELD_TYPE), ex);
        }
    }

    @Override
    public AdditionalDatabaseSettings convertToEntityAttribute(String dbData) {
        try {
            logger.debug("Deserializing JSON to entity. From: {} - {}, To: {}", DB_FIELD_TYPE, dbData, ENTITY_TYPE);
            AdditionalDatabaseSettings attribute = objectMapper.readValue(dbData, AdditionalDatabaseSettings.class);
            logger.debug("Deserialized entity: {}", attribute);
            return attribute;
        } catch (IOException ex) {
            throw new RuntimeException(String.format("Error occurred while deserializing database field. From: %s, To: %s",
                    DB_FIELD_TYPE, ENTITY_TYPE), ex);
        }
    }
}
