package com.example.demo.webUi.webUiFrontModels;

import com.example.demo.models.database.DatabaseType;

public class DatabaseItem {
    private DatabaseType type;

    private int id;

    private String desc;

    private String time;

    public DatabaseItem(DatabaseType type, int id, String desc, String time) {
        this.type = type;
        this.id = id;
        this.desc = desc;
        this.time = time;
    }

    public DatabaseType getType() {
        return type;
    }

    public void setType(DatabaseType type) {
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
