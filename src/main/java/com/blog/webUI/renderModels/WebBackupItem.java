package com.blog.webUI.renderModels;

public class WebBackupItem {
    private int id;

    private String desc;

    private String name;

    private String time;

    public WebBackupItem(int id, String desc, String name, String time) {
        this.id = id;
        this.desc = desc;
        this.name = name;
        this.time = time;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    @Override
    public String toString() {
        return "WebBackupItem{" +
                "id=" + id +
                ", desc='" + desc + '\'' +
                ", name='" + name + '\'' +
                ", time='" + time + '\'' +
                '}';
    }
}
