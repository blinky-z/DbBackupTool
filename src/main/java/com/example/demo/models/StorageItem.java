package com.example.demo.models;

public class StorageItem {
    private String type;

    private String id;

    private String desc;

    private String time;

    public StorageItem(String type, String id, String desc, String time) {
        this.type = type;
        this.id = id;
        this.desc = desc;
        this.time = time;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
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
