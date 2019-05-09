package com.blog.webUI.renderModels;

import com.blog.entities.backup.BackupTaskState;
import com.blog.entities.backup.BackupTaskType;

public class WebBackupTask {
    private Integer id;

    private BackupTaskType type;

    private BackupTaskState state;

    private String time;

    private Boolean isError;

    public WebBackupTask(Integer id, BackupTaskType type, BackupTaskState state, String time, Boolean isError) {
        this.id = id;
        this.type = type;
        this.state = state;
        this.time = time;
        this.isError = isError;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public BackupTaskType getType() {
        return type;
    }

    public void setType(BackupTaskType type) {
        this.type = type;
    }

    public BackupTaskState getState() {
        return state;
    }

    public void setState(BackupTaskState state) {
        this.state = state;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public Boolean getError() {
        return isError;
    }

    public void setError(Boolean error) {
        isError = error;
    }
}
