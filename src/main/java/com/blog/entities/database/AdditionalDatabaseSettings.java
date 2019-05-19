package com.blog.entities.database;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

class AdditionalDatabaseSettings {
    private DatabaseType type;

    private PostgresSettings postgresSettings;

    @JsonCreator
    AdditionalDatabaseSettings(@JsonProperty("type") DatabaseType type,
                               @JsonProperty("postgresSettings") PostgresSettings postgresSettings) {
        this.type = type;
        this.postgresSettings = postgresSettings;
    }

    public DatabaseType getType() {
        return type;
    }

    PostgresSettings getPostgresSettings() {
        return postgresSettings;
    }

    @Override
    public String toString() {
        return "AdditionalDatabaseSettings{" +
                "type=" + type +
                ", postgresSettings=" + postgresSettings +
                '}';
    }
}
