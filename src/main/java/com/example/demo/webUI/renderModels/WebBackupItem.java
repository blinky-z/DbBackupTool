package com.example.demo.webUI.renderModels;

public class WebBackupItem {
    private int id;

    private String desc;

    private String time;

    public WebBackupItem(int id, String desc, String time) {
        this.id = id;
        this.desc = desc;
        this.time = time;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }
}
