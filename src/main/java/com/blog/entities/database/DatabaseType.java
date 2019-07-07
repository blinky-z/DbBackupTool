package com.blog.entities.database;

import java.util.Optional;

/**
 * Database type.
 */
public enum DatabaseType {
    POSTGRES("postgres") {
        public String toString() {
            return "PostgreSQL";
        }
    };

    private final String databaseAsString;

    DatabaseType(String databaseAsString) {
        this.databaseAsString = databaseAsString;
    }

    public static Optional<DatabaseType> of(String database) {
        for (DatabaseType value : values()) {
            if (value.databaseAsString.equals(database)) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }

    public String getDatabaseAsString() {
        return databaseAsString;
    }
}
