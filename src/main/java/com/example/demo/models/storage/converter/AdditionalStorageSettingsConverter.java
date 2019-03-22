package com.example.demo.models.storage.converter;

import com.example.demo.models.storage.AdditionalStorageSettings;
import com.example.demo.models.storage.Storage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.persistence.AttributeConverter;
import java.io.IOException;

public class AdditionalStorageSettingsConverter implements AttributeConverter<AdditionalStorageSettings, String> {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final DropboxSettingsConverter dropboxSettingsConverter = new DropboxSettingsConverter();

    private static final LocalFileSystemSettingsConverter localFileSystemSettingsConverter = new LocalFileSystemSettingsConverter();

    private static final String ENTITY_TYPE = "AdditionalStorageSettings";
    private static final String DB_FIELD_TYPE = "JSON";

    private static final class DbEntity {
        private Storage type;

        private String attribute;
    }

    @Override
    public String convertToDatabaseColumn(AdditionalStorageSettings attribute) {
        DbEntity dbEntity = new DbEntity();
        Storage storageType = attribute.getType();
        switch (storageType) {
            case DROPBOX: {
                dbEntity.type = storageType;
                dbEntity.attribute = dropboxSettingsConverter.convertToDatabaseColumn(attribute.getDropboxSettings());
                break;
            }
            case LOCAL_FILE_SYSTEM: {
                dbEntity.type = storageType;
                dbEntity.attribute = localFileSystemSettingsConverter.convertToDatabaseColumn(attribute.getLocalFileSystemSettings());
                break;
            }
            default: {
                throw new RuntimeException(
                        "Can't convert " + ENTITY_TYPE + " Entity to " + DB_FIELD_TYPE + ". Error: Unknown storage type");
            }
        }

        try {
            return objectMapper.writeValueAsString(dbEntity);
        } catch (JsonProcessingException ex) {
            throw new RuntimeException("Error occurred while serializing " + ENTITY_TYPE + " Entity to " + DB_FIELD_TYPE, ex);
        }
    }

    @Override
    public AdditionalStorageSettings convertToEntityAttribute(String dbData) {
        DbEntity dbEntity;
        try {
            dbEntity = objectMapper.readValue(dbData, DbEntity.class);
        } catch (IOException ex) {
            throw new RuntimeException("Error occurred while deserializing " + DB_FIELD_TYPE + " to " + ENTITY_TYPE + " Entity", ex);
        }

        AdditionalStorageSettings additionalStorageSettings;
        switch (dbEntity.type) {
            case DROPBOX: {
                additionalStorageSettings = AdditionalStorageSettings.
                        dropboxSettings(dropboxSettingsConverter.convertToEntityAttribute(dbEntity.attribute)).build();
                break;
            }
            case LOCAL_FILE_SYSTEM: {
                additionalStorageSettings = AdditionalStorageSettings.localFileSystemSettings(localFileSystemSettingsConverter.
                        convertToEntityAttribute(dbEntity.attribute)).build();
                break;
            }
            default: {
                throw new RuntimeException(
                        "Can't convert " + DB_FIELD_TYPE + " to " + ENTITY_TYPE + " Entity. Error: Unknown storage type");
            }
        }

        return additionalStorageSettings;
    }
}
