package com.example.demo.models.database;

public enum DatabaseType {
    POSTGRES {
        public String toString() {
            return "PostgreSQL";
        }
    }
}
