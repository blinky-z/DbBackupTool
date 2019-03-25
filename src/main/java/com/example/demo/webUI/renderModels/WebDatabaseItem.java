package com.example.demo.webUI.renderModels;

import com.example.demo.entities.database.Database;

public class WebDatabaseItem {
    private int id;

    private Database type;

    private String desc;

    private String time;

    public WebDatabaseItem(Database type, int id, String desc, String time) {
        this.type = type;
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

    public Database getType() {
        return type;
    }

    public void setType(Database type) {
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