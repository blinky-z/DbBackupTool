package com.blog.service.storage;

import com.blog.entities.storage.StorageSettings;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;

/**
 * This interface provides API to work with backup on storages.
 * <p>
 * All storage services implementing this interface should obey the following rules:
 * <ul>
 * <li>Upload/download/delete strategy is based on backup name - backup name is an identifier of the backup.
 * It is not mandatory to use backup name in internal representation, but passing backup name to any of the interface methods should have
 * proper effect.</li>
 * <li>If any of the interface methods run additional threads they should notify about occurred exception using
 * {@link com.blog.service.ErrorCallbackService} and properly release all resources. Main thread can throw an exception directly.</li>
 * </ul>
 *
 * @implSpec Handling of {@link InterruptedException} and {@link java.io.InterruptedIOException}.
 * <ul>
 * <li>
 * Method {@link #uploadBackup(InputStream, StorageSettings, String, Integer)}: this method always run in separate thread.
 * When backup related task is canceled, thread will be interrupted and the output stream on the other side will be closed.
 * So, if you are blocked on {@link InputStream#read()} operation, it will immediately return and throw
 * {@link java.io.InterruptedIOException}. This exception will be thrown even if interrupt occurred before calling of read operation.
 * </li>
 * <li>
 * Method {@link #downloadBackup(StorageSettings, String, Integer)}: this method is not run in separate thread, but in the main thread.
 * If an any exception occurs while executing this method, you can safely return {@literal null}. Also, if {@link InterruptedException}
 * occurs you don't need to re-throw it, but just return {@literal null}.
 * If backup related task is canceled, returned input stream will be closed. That is, if you have a thread that writes to this stream,
 * it will get an {@link java.io.IOException} on next call of {@link java.io.OutputStream#write(int)}.
 * </li>
 * <li>
 * Method {@link #deleteBackup(StorageSettings, String, Integer)}: this method always run in separate thread.
 * If backup related task is canceled, thread will be interrupted.
 * It is not mandatory to check the interrupt flag, so backup can be fully deleted from storage.
 * </li>
 * </ul>
 * @see com.blog.service.TasksStarterService
 * @see com.blog.manager.BackupLoadManager
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
    @Nullable
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
