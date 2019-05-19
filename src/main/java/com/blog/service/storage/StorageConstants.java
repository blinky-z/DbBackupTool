package com.blog.service.storage;

import java.text.SimpleDateFormat;

/**
 * This is utility class that contains various default constants for storage services.
 * <p>
 * Using of these constant is very optional, but desirable to keep uniform style of backups on all storages.
 */
public class StorageConstants {
    /**
     * Date formatter.
     * <p>
     * Example of formatted time: 04-04-2019_15-24-55-839
     */
    public static final SimpleDateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss-SS");
    /**
     * Backup name template to build backup name.
     * <p>
     * The first argument is database name and the second one is creation date, so the full backup name looks like this:
     * <i>backup_&lt;database name&gt;_&lt;creation date&gt;</i>
     * <p>
     * Backup name is created by {@link com.blog.manager.BackupLoadManager} and then passed to required service
     * depending on storage type.
     * <p>
     * Example of backup name of database named "testDb" and creation date "04-04-2019_15-24-55-839":
     * <i>backup_testDb_04-04-2019_15-24-55-839</i>
     */
    public static final String BACKUP_NAME_TEMPLATE = "backup_%s_%s";
    /**
     * Backup files have the following representation:
     * &lt;backup name&gt;_part&lt;backup part&gt;
     * <p>
     * Example of file name: backup_testDb_04-04-2019_15-24-55-839_part0
     */
    static final String DEFAULT_FILENAME_TEMPLATE = "%s_part%d";
    /**
     * Backup files extension.
     * <p>
     * Example of created file: backup_testDb_04-04-2019_15-24-55-839_part0.dat
     */
    static final String DEFAULT_FILE_EXTENSION = ".dat";

    private StorageConstants() {
    }
}
