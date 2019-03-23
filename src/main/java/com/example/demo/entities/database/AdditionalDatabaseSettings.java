package com.example.demo.entities.database;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

class AdditionalDatabaseSettings {
    private Database type;

    private PostgresSettings postgresSettings;

    public Database getType() {
        return type;
    }

    public PostgresSettings getPostgresSettings() {
        return postgresSettings;
    }

    @JsonCreator
    AdditionalDatabaseSettings(@JsonProperty("type") Database type,
                               @JsonProperty("postgresSettings") PostgresSettings postgresSettings) {
        this.type = type;
        this.postgresSettings = postgresSettings;
    }

    @Override
    public String toString() {
        return "AdditionalDatabaseSettings{" +
                "type=" + type +
                ", postgresSettings=" + postgresSettings +
                '}';
    }
}
