package com.blog.service.databaseBackup.PostgresDatabaseBackup.Errors;

/**
 * This exception is thrown when <i>pg_dump</i> or <i>psql</i> reports about error.
 */
public class InternalPostgresToolError extends RuntimeException {
    public InternalPostgresToolError(String error) {
        super(error);
    }
}
