package com.example.demo.entities.storage;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

class AdditionalStorageSettings {
    private Storage type;

    private LocalFileSystemSettings localFileSystemSettings;

    private DropboxSettings dropboxSettings;

    public Storage getType() {
        return type;
    }

    public LocalFileSystemSettings getLocalFileSystemSettings() {
        return localFileSystemSettings;
    }

    public DropboxSettings getDropboxSettings() {
        return dropboxSettings;
    }

    @JsonCreator
    AdditionalStorageSettings(@JsonProperty("type") Storage type,
                              @JsonProperty("localFileSystemSettings") LocalFileSystemSettings localFileSystemSettings,
                              @JsonProperty("dropboxSettings") DropboxSettings dropboxSettings) {
        this.type = type;
        this.localFileSystemSettings = localFileSystemSettings;
        this.dropboxSettings = dropboxSettings;
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
