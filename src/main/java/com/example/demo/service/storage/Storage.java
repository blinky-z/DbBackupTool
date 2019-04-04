package com.example.demo.service.storage;

import com.example.demo.entities.storage.StorageSettings;

import java.io.InputStream;
import java.text.SimpleDateFormat;

/**
 * General Storage interface
 */
public interface Storage {
    /**
     * Date formatter used to build backup name
     *
     * Example of formatted time: 04-04-2019_15-24-55-839
     */
    static final SimpleDateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss-SS");

    /**
     * Backup name have following representation:
     * backup_<database name>_<creation date>
     *
     * Backup name created by <b>BackupLoadManager</b> class
     *
     * Example of backup name: backup_testDb_04-04-2019_15-24-55-839
     */
    static final String BACKUP_NAME_TEMPLATE = "backup_%s_%s";

    /**
     * Backup files have following representation:
     * <backup name>_part<backup part>
     *
     * Example of file name: backup_testDb_04-04-2019_15-24-55-839_part0
     */
    static final String FILENAME_TEMPLATE = "%s_part%d";


    /**
     * Backup files extension
     *
     * Used to create file with extension
     *
     * Example of created file: backup_testDb_04-04-2019_15-24-55-839_part0.dat
     */
    static final String FILE_EXTENSION = ".dat";

    /**
     * Saves backup on specified storage
     *
     * Backup always saved into folder named exactly as backup name
     *
     * The following is a typical uploaded backup:
     * /backup_postgres_04-04-2019_15-24-51-970/backup_postgres_04-04-2019_15-24-51-970_part0.dat
     * /backup_postgres_04-04-2019_15-24-51-970/backup_postgres_04-04-2019_15-24-51-970_part1.dat
     *
     * @param in              the input stream to read backup from
     * @param storageSettings storage settings wraps specified storage settings
     * @param backupName      backup name to create backup folder and backup parts
     */
    public void uploadBackup(InputStream in, StorageSettings storageSettings, String backupName);

    /**
     * Downloads backup from the specified storage
     *
     * @param storageSettings storage settings wraps specified storage settings
     * @param backupName      backup name to retrieve backup folder and backup parts
     *
     * @return input stream, from which backup can be read after download complete
     */
    public InputStream downloadBackup(StorageSettings storageSettings, String backupName);
}
