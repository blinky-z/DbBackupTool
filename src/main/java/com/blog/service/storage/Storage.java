package com.blog.service.storage;

import com.blog.entities.storage.StorageSettings;

import java.io.InputStream;
import java.text.SimpleDateFormat;

/**
 * General Storage interface
 */
public interface Storage {
    /**
     * Date formatter used to build backup name
     * <p>
     * Example of formatted time: 04-04-2019_15-24-55-839
     */
    SimpleDateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss-SS");

    /**
     * Backup name have following representation:
     * backup_<database name>_<creation date>
     * <p>
     * Backup name created by <b>BackupLoadManager</b> class
     * <p>
     * Example of backup name: backup_testDb_04-04-2019_15-24-55-839
     */
    String BACKUP_NAME_TEMPLATE = "backup_%s_%s";

    /**
     * Backup files have following representation:
     * <backup name>_part<backup part>
     * <p>
     * Example of file name: backup_testDb_04-04-2019_15-24-55-839_part0
     */
    String FILENAME_TEMPLATE = "%s_part%d";


    /**
     * Backup files extension
     * <p>
     * Used to create file with extension
     * <p>
     * Example of created file: backup_testDb_04-04-2019_15-24-55-839_part0.dat
     */
    String FILE_EXTENSION = ".dat";

    /**
     * Saves backup on specified storage
     * <p>
     * Backup always saved into folder named exactly as backup name
     * <p>
     * The following is a typical uploaded backup:
     * /backup_postgres_04-04-2019_15-24-51-970/backup_postgres_04-04-2019_15-24-51-970_part0.dat
     * /backup_postgres_04-04-2019_15-24-51-970/backup_postgres_04-04-2019_15-24-51-970_part1.dat
     *
     * @param in              the input stream to read backup from
     * @param storageSettings storage settings wraps specified storage settings
     * @param backupName      backup name to create backup folder and backup parts
     */
    void uploadBackup(InputStream in, StorageSettings storageSettings, String backupName);

    /**
     * Downloads backup from the specified storage
     *
     * @param storageSettings storage settings wraps specified storage settings
     * @param backupName      backup name to retrieve backup folder and backup parts
     * @return input stream, from which backup can be read after download complete
     */
    InputStream downloadBackup(StorageSettings storageSettings, String backupName);

    void deleteBackup(StorageSettings storageSettings, String backupName);
}
