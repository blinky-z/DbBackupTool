package com.example.demo.webUI.renderModels;

import com.example.demo.entities.storage.StorageType;

public class WebStorageItem {
    private StorageType type;

    private int id;

    private String desc;

    private String time;

    public WebStorageItem(StorageType type, int id, String desc, String time) {
        this.type = type;
        this.id = id;
        this.desc = desc;
        this.time = time;
    }

    public StorageType getType() {
        return type;
    }

    public void setType(StorageType type) {
        this.type = type;
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
