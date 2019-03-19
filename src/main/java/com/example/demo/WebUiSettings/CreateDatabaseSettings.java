package com.example.demo.WebUiSettings;

public class CreateDatabaseSettings {
    private String databaseType;

    private DatabaseSettings databaseSettings;

    public String getDatabaseType() {
        return databaseType;
    }

    public void setDatabaseType(String databaseType) {
        this.databaseType = databaseType;
    }

    public DatabaseSettings getDatabaseSettings() {
        return databaseSettings;
    }

    public void setDatabaseSettings(DatabaseSettings databaseSettings) {
        this.databaseSettings = databaseSettings;
    }
}
