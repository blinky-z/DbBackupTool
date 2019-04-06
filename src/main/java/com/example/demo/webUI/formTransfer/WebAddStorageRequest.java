package com.example.demo.webUI.formTransfer;

import com.example.demo.webUI.formTransfer.storage.WebDropboxSettings;
import com.example.demo.webUI.formTransfer.storage.WebLocalFileSystemSettings;

public class WebAddStorageRequest {
    private String storageType;

    private String settingsName;

    private WebLocalFileSystemSettings localFileSystemSettings;

    private WebDropboxSettings dropboxSettings;

    public String getStorageType() {
        return storageType;
    }

    public void setStorageType(String storageType) {
        this.storageType = storageType;
    }

    public String getSettingsName() {
        return settingsName;
    }

    public void setSettingsName(String settingsName) {
        this.settingsName = settingsName;
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
