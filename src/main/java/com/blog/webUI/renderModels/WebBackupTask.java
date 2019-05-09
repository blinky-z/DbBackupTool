package com.blog.webUI.renderModels;

public class WebBackupTask {
    private Integer id;

    private String type;

    private String state;

    private String time;

    private Boolean isError;

    public WebBackupTask(Integer id, String type, String state, String time, Boolean isError) {
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
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
