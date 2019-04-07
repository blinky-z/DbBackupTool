package com.blog.webUI.renderModels;

import com.blog.entities.storage.StorageType;

public class WebStorageItem {
    private String settingsName;

    private StorageType type;

    private String desc;

    private String time;

    public WebStorageItem(StorageType type, String settingsName, String desc, String time) {
        this.type = type;
        this.settingsName = settingsName;
        this.desc = desc;
        this.time = time;
    }

    public StorageType getType() {
        return type;
    }

    public void setType(StorageType type) {
        this.type = type;
    }

    public String getSettingsName() {
        return settingsName;
    }

    public void setSettingsName(String settingsName) {
        this.settingsName = settingsName;
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
