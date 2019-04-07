package com.blog.webUI.renderModels;

import com.blog.entities.database.DatabaseType;

public class WebDatabaseItem {
    private String settingsName;

    private DatabaseType type;

    private String desc;

    private String time;

    public WebDatabaseItem(DatabaseType type, String settingsName, String desc, String time) {
        this.type = type;
        this.settingsName = settingsName;
        this.desc = desc;
        this.time = time;
    }

    public String getSettingsName() {
        return settingsName;
    }

    public void setSettingsName(String settingsName) {
        this.settingsName = settingsName;
    }

    public DatabaseType getType() {
        return type;
    }

    public void setType(DatabaseType type) {
        this.type = type;
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
