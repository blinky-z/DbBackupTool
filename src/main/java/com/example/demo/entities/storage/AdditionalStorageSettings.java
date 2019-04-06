package com.example.demo.entities.storage;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

class AdditionalStorageSettings {
    private StorageType type;

    private LocalFileSystemSettings localFileSystemSettings;

    private DropboxSettings dropboxSettings;

    @JsonCreator
    AdditionalStorageSettings(@JsonProperty("type") StorageType type,
                              @JsonProperty("localFileSystemSettings") LocalFileSystemSettings localFileSystemSettings,
                              @JsonProperty("dropboxSettings") DropboxSettings dropboxSettings) {
        this.type = type;
        this.localFileSystemSettings = localFileSystemSettings;
        this.dropboxSettings = dropboxSettings;
    }

    public StorageType getType() {
        return type;
    }

    public LocalFileSystemSettings getLocalFileSystemSettings() {
        return localFileSystemSettings;
    }

    public DropboxSettings getDropboxSettings() {
        return dropboxSettings;
    }

    @Override
    public String toString() {
        return "AdditionalStorageSettings{" +
                "type=" + type +
                ", localFileSystemSettings=" + localFileSystemSettings +
                ", dropboxSettings=" + dropboxSettings +
                '}';
    }
}
