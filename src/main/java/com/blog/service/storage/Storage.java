package com.blog.service.storage;

import com.blog.entities.storage.StorageSettings;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;

/**
 * This interface provides API to work with backup on storages.
 * <p>
 * All storage services implementing this interface should obey the following rules:
 * <ul>
 * <li>Upload/download/delete strategy is based on backup name - backup name is an identifier of the backup.
 * It is not mandatory to use backup name in internal representation, but passing backup name to any of interface methods
 * backup should have proper effect.</li>
 * <li>If service uses additional threads they should notify about occurred exception using
 * {@link com.blog.service.ErrorCallbackService} if the exception is critical and work can not be continued.</li>
 * </ul>
 *
 * @see StorageConstants
 */
public interface Storage {
    /**
     * Saves backup on storage.
     *
     * @param in              the input stream to read backup from
     * @param storageSettings storage settings to access storage where backup stored
     * @param backupName      backup name
     * @param id              backup upload task ID
     */
    void uploadBackup(InputStream in, StorageSettings storageSettings, String backupName, Integer id);

    /**
     * Downloads backup from storage.
     *
     * @param storageSettings storage settings to access storage where backup stored
     * @param backupName      backup name
     * @param id              backup download task ID
     * @return input stream, from which backup can be read after download complete
     */
    @NotNull
    InputStream downloadBackup(StorageSettings storageSettings, String backupName, Integer id);

    /**
     * Deletes backup from storage.
     *
     * @param storageSettings storage settings to access storage where backup stored
     * @param backupName      backup name
     * @param id              backup deletion task ID
     */
    void deleteBackup(StorageSettings storageSettings, String backupName, Integer id);
}
