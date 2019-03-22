package com.example.demo.entities.database;

import java.util.Optional;

public enum Database {
    POSTGRES("postgres") {
        public String toString() {
            return "PostgreSQL";
        }
    };

    private final String databaseAsString;

    Database(String databaseAsString) {
        this.databaseAsString = databaseAsString;
    }

    public static Optional<Database> of(String database) {
        for (Database value : values()) {
            if (value.databaseAsString.equals(database)) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }
}
