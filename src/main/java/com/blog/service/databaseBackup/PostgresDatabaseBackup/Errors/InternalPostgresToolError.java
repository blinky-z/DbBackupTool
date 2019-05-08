package com.blog.service.databaseBackup.PostgresDatabaseBackup.Errors;

public class InternalPostgresToolError extends RuntimeException {
    public InternalPostgresToolError(String error) {
        super(error);
    }
}
