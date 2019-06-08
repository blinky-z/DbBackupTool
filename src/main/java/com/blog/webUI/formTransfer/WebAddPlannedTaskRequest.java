package com.blog.webUI.formTransfer;

import java.util.List;

/**
 * Data transfer object for adding planned task form.
 * <p>
 * Passed to router "/add-planned-task" on POST request.
 */
public class WebAddPlannedTaskRequest {
    private String databaseSettingsName;

    private List<String> storageSettingsNameList;

    private List<String> processors;

    private String interval;

    public String getDatabaseSettingsName() {
        return databaseSettingsName;
    }

    public void setDatabaseSettingsName(String databaseSettingsName) {
        this.databaseSettingsName = databaseSettingsName;
    }

    public List<String> getStorageSettingsNameList() {
        return storageSettingsNameList;
    }

    public void setStorageSettingsNameList(List<String> storageSettingsNameList) {
        this.storageSettingsNameList = storageSettingsNameList;
    }

    public List<String> getProcessors() {
        return processors;
    }

    public void setProcessors(List<String> processors) {
        this.processors = processors;
    }

    public String getInterval() {
        return interval;
    }

    public void setInterval(String interval) {
        this.interval = interval;
    }

    @Override
    public String toString() {
        return "WebAddPlannedTaskRequest{" +
                "databaseSettingsName='" + databaseSettingsName + '\'' +
                ", storageSettingsNameList=" + storageSettingsNameList +
                ", processors=" + processors +
                ", interval='" + interval + '\'' +
                '}';
    }
}
