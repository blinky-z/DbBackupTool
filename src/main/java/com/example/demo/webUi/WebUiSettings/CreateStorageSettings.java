package com.example.demo.webUi.WebUiSettings;

import com.example.demo.models.storage.DropboxSettings;
import com.example.demo.models.storage.LocalFileSystemSettings;

public class CreateStorageSettings {
    private String storageType;

    private DropboxSettings dropboxSettings;

    private LocalFileSystemSettings localFileSystemSettings;

    public String getStorageType() {
        return storageType;
    }

    public void setStorageType(String storageType) {
        this.storageType = storageType;
    }

    public DropboxSettings getDropboxSettings() {
        return dropboxSettings;
    }

    public void setDropboxSettings(DropboxSettings dropboxSettings) {
        this.dropboxSettings = dropboxSettings;
    }

    public LocalFileSystemSettings getLocalFileSystemSettings() {
        return localFileSystemSettings;
    }

    public void setLocalFileSystemSettings(LocalFileSystemSettings localFileSystemSettings) {
        this.localFileSystemSettings = localFileSystemSettings;
    }
}
