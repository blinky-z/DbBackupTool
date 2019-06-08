package com.blog.webUI.formTransfer;

/**
 * This class represents restoring database backup form
 * <p>
 * Passed to router "/restore-backup" on POST request
 */
public class WebRestoreBackupRequest {
    private String backupId;

    private String databaseSettingsName;

    private String storageSettingsName;

    public String getBackupId() {
        return backupId;
    }

    public void setBackupId(String backupId) {
        this.backupId = backupId;
    }

    public String getDatabaseSettingsName() {
        return databaseSettingsName;
    }

    public void setDatabaseSettingsName(String databaseSettingsName) {
        this.databaseSettingsName = databaseSettingsName;
    }

    public String getStorageSettingsName() {
        return storageSettingsName;
    }

    public void setStorageSettingsName(String storageSettingsName) {
        this.storageSettingsName = storageSettingsName;
    }

    @Override
    public String toString() {
        return "WebRestoreBackupRequest{" +
                "backupId='" + backupId + '\'' +
                ", databaseSettingsName='" + databaseSettingsName + '\'' +
                ", storageSettingsName='" + storageSettingsName + '\'' +
                '}';
    }
}
