package com.blog.manager;

import com.blog.entities.database.DatabaseSettings;
import com.blog.entities.database.DatabaseType;
import com.blog.service.databaseBackup.PostgresDatabaseBackup.PostgresDatabaseBackup;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Objects;

/**
 * This manager class provides API to work with database backups.
 */
@Component
public class DatabaseBackupManager {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseBackupManager.class);

    private PostgresDatabaseBackup postgresDatabaseBackup;

    @Autowired
    public void setPostgresDatabaseBackup(PostgresDatabaseBackup postgresDatabaseBackup) {
        this.postgresDatabaseBackup = postgresDatabaseBackup;
    }

    /**
     * Created database backup.
     *
     * @param databaseSettings database settings of database to dump
     * @param id               create backup task ID
     * @return input stream or {@code null} if thread was interrupted
     */
    @NotNull
    public InputStream createBackup(@NotNull DatabaseSettings databaseSettings, @NotNull Integer id) {
        Objects.requireNonNull(databaseSettings);
        Objects.requireNonNull(id);

        logger.info("Creating backup of database {}... Database Settings: {}", databaseSettings.getName(), databaseSettings);

        InputStream backupStream;

        DatabaseType databaseType = databaseSettings.getType();
        switch (databaseType) {
            case POSTGRES: {
                backupStream = postgresDatabaseBackup.createBackup(databaseSettings, id);
                break;
            }
            default: {
                throw new RuntimeException(String.format("Can't create backup. Unknown database type: %s", databaseType));
            }
        }

        logger.info("Backup creation started. Database name: {}", databaseSettings.getName());

        return backupStream;
    }

    /**
     * Restores database backup.
     *
     * @param in               input stream, from which plain text backup can be read.
     * @param databaseSettings database settings of database to restore backup to
     * @param id               restore backup task ID
     */
    public void restoreBackup(@NotNull InputStream in, @NotNull DatabaseSettings databaseSettings, @NotNull Integer id) {
        Objects.requireNonNull(in);
        Objects.requireNonNull(databaseSettings);
        Objects.requireNonNull(id);

        logger.info("Restoring backup to database {}... Database Settings: {}", databaseSettings.getName(), databaseSettings);

        DatabaseType databaseType = databaseSettings.getType();
        switch (databaseType) {
            case POSTGRES: {
                postgresDatabaseBackup.restoreBackup(in, databaseSettings, id);
                break;
            }
            default: {
                throw new RuntimeException(String.format("Can't restore backup. Unknown database type: %s", databaseType));
            }
        }

        logger.info("Backup successfully restored. Database name: {}", databaseSettings.getName());
    }
}
