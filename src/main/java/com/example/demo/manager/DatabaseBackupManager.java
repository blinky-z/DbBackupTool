package com.example.demo.manager;

import com.example.demo.service.databaseBackup.PostgresDatabaseBackup;
import com.example.demo.entities.database.DatabaseSettings;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Component
public class DatabaseBackupManager {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseBackupManager.class);

    public InputStream createBackup(@NotNull DatabaseSettings databaseSettings) {
        logger.info("Creating new backup: Database Settings: {}", databaseSettings);

        switch (databaseSettings.getType()) {
            case POSTGRES: {
                PostgresDatabaseBackup postgresBackupCreator = new PostgresDatabaseBackup(databaseSettings);
                return postgresBackupCreator.createDbDump();
            }
            default: {
                throw new RuntimeException("Can't create backup: Unknown database type");
            }
        }
    }
}
