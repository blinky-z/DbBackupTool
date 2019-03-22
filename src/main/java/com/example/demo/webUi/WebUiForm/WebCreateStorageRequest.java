package com.example.demo.webUi.WebUiForm;

import com.example.demo.entities.storage.DropboxSettings;
import com.example.demo.entities.storage.LocalFileSystemSettings;
import com.example.demo.webUi.WebUiForm.storage.WebDropboxSettings;
import com.example.demo.webUi.WebUiForm.storage.WebLocalFileSystemSettings;

public class WebCreateStorageRequest {
    private String storageType;

    private WebLocalFileSystemSettings localFileSystemSettings;

    private WebDropboxSettings dropboxSettings;

    public String getStorageType() {
        return storageType;
    }

    public void setStorageType(String storageType) {
        this.storageType = storageType;
    }

    public WebLocalFileSystemSettings getLocalFileSystemSettings() {
        return localFileSystemSettings;
    }

    public void setLocalFileSystemSettings(WebLocalFileSystemSettings localFileSystemSettings) {
        this.localFileSystemSettings = localFileSystemSettings;
    }

    public WebDropboxSettings getDropboxSettings() {
        return dropboxSettings;
    }

    public void setDropboxSettings(WebDropboxSettings dropboxSettings) {
        this.dropboxSettings = dropboxSettings;
    }
}
