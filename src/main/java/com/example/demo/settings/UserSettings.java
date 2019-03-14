package com.example.demo.settings;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "user-config")
public class UserSettings {
    private String backupDir;

    private String dropboxAccessToken;

    public String getBackupDir() {
        return backupDir;
    }

    public void setBackupDir(String backupDir) {
        this.backupDir = backupDir;
    }

    public String getDropboxAccessToken() {
        return dropboxAccessToken;
    }

    public void setDropboxAccessToken(String dropboxAccessToken) {
        this.dropboxAccessToken = dropboxAccessToken;
    }
}
