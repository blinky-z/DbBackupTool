package com.blog.webUI.formTransfer;

import java.util.List;

/**
 * This class represents database backup creation form.
 * <p>
 * Passed to router "/create-backup" on POST request
 */
public class WebCreateBackupRequest {
    private String databaseSettingsName;

    private List<String> processors;

    private List<String> storageSettingsNameList;

    public String getDatabaseSettingsName() {
        return databaseSettingsName;
    }

    public void setDatabaseSettingsName(String databaseSettingsName) {
        this.databaseSettingsName = databaseSettingsName;
    }

    public List<String> getProcessors() {
        return processors;
    }

    public void setProcessors(List<String> processors) {
        this.processors = processors;
    }

    public List<String> getStorageSettingsNameList() {
        return storageSettingsNameList;
    }

    public void setStorageSettingsNameList(List<String> storageSettingsNameList) {
        this.storageSettingsNameList = storageSettingsNameList;
    }
}
