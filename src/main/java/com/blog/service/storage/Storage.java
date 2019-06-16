package com.blog.service.storage;

import com.blog.entities.storage.StorageSettings;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;

/**
 * This interface provides API to manage backup on storage.
 *
 * @implSpec All classes implementing this interface should obey the following rules:
 * <ul>
 * <li>Upload/download/delete strategy is based on backup name - backup name is an identifier of the backup.
 * It is not mandatory to use backup name in internal representation, but passing backup name to any of the interface methods should have
 * proper effect.</li>
 * <li>If any of the interface methods run additional threads these threads should notify about occurred exception using
 * {@link com.blog.service.ErrorCallbackService} and properly release all resources.</li>
 * <li>Main thread should throw an exception directly (wrapped in {@link RuntimeException})</li>
 * </ul>
 * <p>
 * <b>Handling of {@link InterruptedException} and {@link java.io.InterruptedIOException}:</b>
 * <p>
 * The thread where any of the interface functions is executing may be interrupted.
 * <p>
 * Usually you don't want to check the interrupt flag manually, because if you <b>attempt to call</b> or already called any operation that
 * can throw {@link InterruptedException} or any blocking I/O operation and thread was interrupted (either before calling or while thread
 * was blocked on the call) {@link InterruptedException} or {@link java.io.InterruptedIOException} exception will be thrown.
 * <p>
 * If an interrupt occurs, you should properly stop the work and make all the additional threads stop the work too (if any run).
 * You don't need to report (throwing an exception or using {@link com.blog.service.ErrorCallbackService}) about occurred interrupt neither
 * from the main thread nor additional threads.
 * <p>
 * You <b>do not</b> need to revert any operation when interrupt occurs.
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
     * @param id              backup uploading task ID
     * @implSpec This method should not return until backup will be fully uploaded or exception occurs.
     */
    void uploadBackup(InputStream in, StorageSettings storageSettings, String backupName, Integer id);

    /**
     * Downloads backup from storage.
     *
     * @param storageSettings storage settings to access storage where backup stored
     * @param backupName      backup name
     * @param id              backup downloading task ID
     * @return input stream, from which backup can be read
     * @implSpec This method may return while backup is still downloading (e.g. downloading is streaming).
     * <p>
     * if an interrupt occurs, you can safely return {@literal null}.
     * <p>
     * if any exception occurs while working with the returned input stream, it is guaranteed that this input stream will be closed.
     * So if you have a thread that writes to this stream, the thread will get an {@link java.io.IOException} when attempts to write to the
     * closed stream.
     */
    @Nullable
    InputStream downloadBackup(StorageSettings storageSettings, String backupName, Integer id);

    /**
     * Deletes backup from storage.
     *
     * @param storageSettings storage settings to access storage where backup stored
     * @param backupName      backup name
     * @param id              backup deletion task ID
     * @implSpec This method should not return until backup will be fully deleted or exception occurred.
     * <p>
     * You can ignore interrupts and delete backup fully.
     */
    void deleteBackup(StorageSettings storageSettings, String backupName, Integer id);
}
