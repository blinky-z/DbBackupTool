package com.blog.service.databaseBackup;

import com.blog.entities.database.DatabaseSettings;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;

/**
 * This interface provides API to work with database backups.
 */
public interface DatabaseBackup {
    /**
     * Created database backup.
     *
     * @param databaseSettings database settings of database to dump
     * @param id               create backup task ID
     * @return input stream, from which created plain text backup can be read.
     */
    InputStream createBackup(@NotNull DatabaseSettings databaseSettings, @NotNull Integer id);

    /**
     * Restores database backup.
     *
     * @param in               input stream, from which plain text backup can be read.
     * @param databaseSettings database settings of database to restore backup to
     * @param id               restore backup task ID
     */
    void restoreBackup(InputStream in, @NotNull DatabaseSettings databaseSettings, @NotNull Integer id);
}
