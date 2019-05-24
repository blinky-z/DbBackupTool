package com.blog.webUI.formTransfer;

import java.util.ArrayList;
import java.util.List;

/**
 * Data transfer object for adding planned task form.
 * <p>
 * Passed to router "/add-planned-task" on POST request.
 */
public class WebAddPlannedTaskRequest {
    private String taskType;

    private String databaseSettingsName;

    private List<String> storageSettingsNameList = new ArrayList<>();

    private List<String> processors = new ArrayList<>();

    private String interval;

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

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
}
