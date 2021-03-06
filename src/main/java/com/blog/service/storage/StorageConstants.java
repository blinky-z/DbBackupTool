package com.blog.service.storage;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * This is utility class that contains various default constants that can be useful at implementing {@link Storage} interface.
 * <p>
 * Using of these constant is optional, but desirable to keep uniform style of backups on all storages.
 */
public class StorageConstants {
    /**
     * Time formatter.
     * <p>
     * The instance of {@link DateTimeFormatter} is thread-safe.
     * <p>
     * Example of formatted time: {@literal 04-04-2019_15-24-55-839}
     */
    public static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy_HH-mm-ss-SS");
    /**
     * Backup name template.
     * <p>
     * The first argument is database name and the second one is creation date, so the full backup name looks like this:
     * {@code backup_<database name>_<creation date>}
     * <p>
     * Usually don't want to use it.
     * This constant is used by {@link com.blog.manager.BackupPropertiesManager#initNewBackupProperties(List, List, String)} method to
     * create backup name.
     * That is, it is guaranteed that {@code backupName} parameter in one of the {@link Storage} methods is always of this format.
     * <p>
     * Example of backup name of database named "testDb" and with backup creation date "04-04-2019_15-24-55-839":
     * {@literal backup_testDb_04-04-2019_15-24-55-839}
     */
    public static final String BACKUP_NAME_TEMPLATE = "backup_%s_%s";
    /**
     * Default file name template.
     * <p>
     * Backup files have the following representation:
     * {@code <backup name>_part<backup part>}
     * <p>
     * Example of file name: {@literal backup_testDb_04-04-2019_15-24-55-839_part0}
     */
    static final String DEFAULT_FILENAME_TEMPLATE = "%s_part%d";
    /**
     * Default backup files extension.
     */
    static final String DEFAULT_FILE_EXTENSION = ".dat";

    private StorageConstants() {
    }
}
