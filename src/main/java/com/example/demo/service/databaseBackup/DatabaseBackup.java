package com.example.demo.service.databaseBackup;

import com.example.demo.entities.database.DatabaseSettings;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;

/**
 * This interface provides API to work with backups of any database.
 */
public interface DatabaseBackup {
    /**
     * Creates database backup.
     *
     * @return input stream contains the plain text database backup
     */
    InputStream createBackup(@NotNull DatabaseSettings databaseSettings);

    /**
     * Restores database backup.
     *
     * @param dumpData input stream contains the plain text database backup
     */
    void restoreBackup(InputStream dumpData, @NotNull DatabaseSettings databaseSettings);
}
