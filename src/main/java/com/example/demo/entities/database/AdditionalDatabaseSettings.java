package com.example.demo.entities.database;

class AdditionalDatabaseSettings {
    private Database type;

    private PostgresSettings postgresSettings;

    public Database getType() {
        return type;
    }

    public void setType(Database type) {
        this.type = type;
    }

    public PostgresSettings getPostgresSettings() {
        return postgresSettings;
    }

    public void setPostgresSettings(PostgresSettings postgresSettings) {
        this.postgresSettings = postgresSettings;
    }

    AdditionalDatabaseSettings(Database type, PostgresSettings postgresSettings) {
        this.type = type;
        this.postgresSettings = postgresSettings;
    }
}
