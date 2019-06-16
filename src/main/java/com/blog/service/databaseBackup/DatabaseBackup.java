package com.blog.service.databaseBackup;

import com.blog.entities.database.DatabaseSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;

/**
 * This interface provides API to work with database backups.
 *
 * @implSpec All classes implementing this interface should obey the following rules:
 * <ul>
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
 * @see com.blog.service.TasksStarterService
 */
public interface DatabaseBackup {
    /**
     * Created database backup.
     *
     * @param databaseSettings database settings of database to dump
     * @param id               create backup task ID
     * @return input stream, from which created plain text backup can be read.
     * @implSpec This method may return while backup is still creating (e.g. process or thread writes to the returned input stream).
     * <p>
     * if an interrupt occurs before returning result, you can safely return {@literal null}.
     * <p>
     * if any exception occurs while working with the returned input stream, it is guaranteed that this input stream will be closed.
     * So if you have a thread that writes to this stream, the thread will get an {@link java.io.IOException} when attempts to write to the
     * closed stream.
     */
    @Nullable
    InputStream createBackup(@NotNull DatabaseSettings databaseSettings, @NotNull Integer id);

    /**
     * Restores database backup.
     *
     * @param in               input stream, from which plain text backup can be read.
     * @param databaseSettings database settings of database to restore backup to
     * @param id               restore backup task ID
     * @implSpec This method should not return until backup will be fully restored or exception occurred.
     * <p>
     * Consider perform restoring in single transaction to prevent situation when database will be left in inconsistent state if server
     * shutdown or interrupts occurs. If there are any exception transaction should be discarded.
     */
    void restoreBackup(InputStream in, @NotNull DatabaseSettings databaseSettings, @NotNull Integer id);
}
