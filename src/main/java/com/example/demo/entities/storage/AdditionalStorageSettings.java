package com.example.demo.entities.storage;

class AdditionalStorageSettings {
    private Storage type;

    private LocalFileSystemSettings localFileSystemSettings;

    private DropboxSettings dropboxSettings;

    public Storage getType() {
        return type;
    }

    LocalFileSystemSettings getLocalFileSystemSettings() {
        return localFileSystemSettings;
    }

    DropboxSettings getDropboxSettings() {
        return dropboxSettings;
    }

    AdditionalStorageSettings(Storage type, LocalFileSystemSettings localFileSystemSettings, DropboxSettings dropboxSettings) {
        this.type = type;
        this.localFileSystemSettings = localFileSystemSettings;
        this.dropboxSettings = dropboxSettings;
    }
}
