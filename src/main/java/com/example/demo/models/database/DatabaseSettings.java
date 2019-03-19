package com.example.demo.models.database;

import java.util.Date;
import java.util.HashMap;

public interface DatabaseSettings {
    public int getId();

    public DatabaseType getDatabaseType();

    public HashMap<String, String> getProperties();

    public Date getDate();
}
