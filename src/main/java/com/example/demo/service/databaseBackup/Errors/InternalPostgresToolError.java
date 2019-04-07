package com.example.demo.service.databaseBackup.Errors;

public class InternalPostgresToolError extends RuntimeException {
    public InternalPostgresToolError(String error) {
        super(error);
    }
}
