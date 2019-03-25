package com.example.demo.webUI.formTransfer;

public class WebRestoreBackupRequest {
    private Integer backupId;

    private Integer databaseId;

    public Integer getBackupId() {
        return backupId;
    }

    public void setBackupId(Integer backupId) {
        this.backupId = backupId;
    }

    public Integer getDatabaseId() {
        return databaseId;
    }

    public void setDatabaseId(Integer databaseId) {
        this.databaseId = databaseId;
    }
}
