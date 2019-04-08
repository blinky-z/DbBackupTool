package com.blog.manager;

import com.blog.entities.database.DatabaseSettings;
import com.blog.entities.database.DatabaseType;
import com.blog.service.databaseBackup.PostgresDatabaseBackup;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Objects;

@Component
public class DatabaseBackupManager {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseBackupManager.class);

    private PostgresDatabaseBackup postgresDatabaseBackup;

    @Autowired
    public void setPostgresDatabaseBackup(PostgresDatabaseBackup postgresDatabaseBackup) {
        this.postgresDatabaseBackup = postgresDatabaseBackup;
    }

    public InputStream createBackup(@NotNull DatabaseSettings databaseSettings) {
        Objects.requireNonNull(databaseSettings);
        logger.info("Creating backup of database {}... Database Settings: {}", databaseSettings.getName(), databaseSettings);

        InputStream backupStream;

        DatabaseType databaseType = databaseSettings.getType();
        switch (databaseType) {
            case POSTGRES: {
                backupStream = postgresDatabaseBackup.createBackup(databaseSettings);
                break;
            }
            default: {
                throw new RuntimeException(String.format("Can't create backup. Unknown database type: %s", databaseType));
            }
        }

        logger.info("Backup successfully created. Database name: {}", databaseSettings.getName());

        return backupStream;
    }

    public void restoreBackup(@NotNull InputStream backup, @NotNull DatabaseSettings databaseSettings) {
        Objects.requireNonNull(backup);
        Objects.requireNonNull(databaseSettings);
        logger.info("Restoring backup to database {}... Database Settings: {}", databaseSettings.getName(), databaseSettings);

        DatabaseType databaseType = databaseSettings.getType();
        switch (databaseType) {
            case POSTGRES: {
                postgresDatabaseBackup.restoreBackup(backup, databaseSettings);
                break;
            }
            default: {
                throw new RuntimeException(String.format("Can't restore backup. Unknown database type: %s", databaseType));
            }
        }

        logger.info("Backup successfully restored. Database name: {}", databaseSettings.getName());
    }
}
