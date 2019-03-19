package com.example.demo.webUi.WebUiSettings;

import com.example.demo.models.database.PostgresSettings;

public class CreateDatabaseSettings {
    private String databaseType;

    private PostgresSettings postgresSettings;

    public String getDatabaseType() {
        return databaseType;
    }

    public void setDatabaseType(String databaseType) {
        this.databaseType = databaseType;
    }

    public PostgresSettings getPostgresSettings() {
        return postgresSettings;
    }

    public void setPostgresSettings(PostgresSettings postgresSettings) {
        this.postgresSettings = postgresSettings;
    }
}
