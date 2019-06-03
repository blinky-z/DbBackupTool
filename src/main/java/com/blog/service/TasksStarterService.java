package com.blog.service;

import com.blog.entities.backup.BackupProperties;
import com.blog.entities.database.DatabaseSettings;
import com.blog.entities.storage.StorageSettings;
import com.blog.entities.task.Task;
import com.blog.manager.*;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.SimpleLock;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * This class provides API to start tasks: creation, restoration of deletion of backup.
 */
@Component
public class TasksStarterService {
    private static final Duration lockTimeout = Duration.ofHours(24);
    private static final String taskLockPrefix = "taskLock";
    private final ConcurrentHashMap<Integer, Future> futures = new ConcurrentHashMap<>();

    private ExecutorService tasksStarterExecutorService;
    private TasksManager tasksManager;
    private DatabaseBackupManager databaseBackupManager;
    private BackupProcessorManager backupProcessorManager;
    private BackupLoadManager backupLoadManager;
    private ErrorTasksManager errorTasksManager;
    private JdbcTemplateLockProvider jdbcTemplateLockProvider;

    @Autowired
    public void setTasksStarterExecutorService(ExecutorService tasksStarterExecutorService) {
        this.tasksStarterExecutorService = tasksStarterExecutorService;
    }

    @Autowired
    public void setTasksManager(TasksManager tasksManager) {
        this.tasksManager = tasksManager;
    }

    @Autowired
    public void setDatabaseBackupManager(DatabaseBackupManager databaseBackupManager) {
        this.databaseBackupManager = databaseBackupManager;
    }

    @Autowired
    public void setBackupProcessorManager(BackupProcessorManager backupProcessorManager) {
        this.backupProcessorManager = backupProcessorManager;
    }

    @Autowired
    public void setBackupLoadManager(BackupLoadManager backupLoadManager) {
        this.backupLoadManager = backupLoadManager;
    }

    @Autowired
    public void setErrorTasksManager(ErrorTasksManager errorTasksManager) {
        this.errorTasksManager = errorTasksManager;
    }

    @Autowired
    public void setJdbcTemplateLockProvider(JdbcTemplateLockProvider jdbcTemplateLockProvider) {
        this.jdbcTemplateLockProvider = jdbcTemplateLockProvider;
    }

    private LockConfiguration getNewLockConfiguration(Integer taskId) {
        return new LockConfiguration(taskLockPrefix + taskId, Instant.now().plus(lockTimeout));
    }

    /**
     * Returns the {@literal Future} related to specified {@link Task}.
     * <p>
     * Usually you want to get {@literal} Future to cancel task.
     *
     * @param taskId {@link Task} entity's ID
     * @return the Future or {@literal Optional#empty()} if none found
     */
    public Optional<Future> getFuture(@NotNull Integer taskId) {
        return Optional.ofNullable(futures.get(taskId));
    }

    /**
     * Starts database backup creation task.
     * <p>
     * Also sets lock on task. When task is interrupted or completed lock will be released.
     * If server shutdown while lock was hold, lock will be released automatically in time described by {@link #lockTimeout} constant.
     *
     * @param taskId           pre-created {@link Task} entity's ID
     * @param backupProperties pre-created backup properties
     * @param databaseSettings database settings
     * @param logger           the logger
     * @return {@literal Future} of started task
     * @see BackupPropertiesManager#initNewBackupProperties(StorageSettings, List, String)
     * @see TasksManager#initNewTask(Task.Type, Task.RunType, BackupProperties)
     */
    public Future startBackupTask(@NotNull Integer taskId, @NotNull BackupProperties backupProperties,
                                  @NotNull DatabaseSettings databaseSettings, @NotNull Logger logger) {
        Objects.requireNonNull(taskId);
        Objects.requireNonNull(backupProperties);
        Objects.requireNonNull(databaseSettings);
        Objects.requireNonNull(logger);

        Future future = tasksStarterExecutorService.submit(() -> {
            logger.debug("Acquiring a lock... Task ID: {}", taskId);
            SimpleLock lock = jdbcTemplateLockProvider.lock(getNewLockConfiguration(taskId))
                    .orElseThrow(() -> new IllegalStateException("Lock can't be acquired"));
            logger.debug("Lock acquired. Task ID: {}", taskId);

            tasksManager.updateTaskState(taskId, Task.State.CREATING);
            logger.info("Creating backup...");

            try (InputStream backupStream = databaseBackupManager.createBackup(databaseSettings, taskId)) {
                if (Thread.interrupted()) {
                    tasksManager.updateTaskState(taskId, Task.State.INTERRUPTED);
                    throw new InterruptedException();
                }

                tasksManager.updateTaskState(taskId, Task.State.APPLYING_PROCESSORS);
                List<String> processors = backupProperties.getProcessors();
                logger.info("Applying processors on created backup. Processors: {}", processors);

                InputStream processedBackupStream = backupProcessorManager.process(backupStream, processors);
                if (Thread.interrupted()) {
                    tasksManager.updateTaskState(taskId, Task.State.INTERRUPTED);
                    throw new InterruptedException();
                }

                tasksManager.updateTaskState(taskId, Task.State.UPLOADING);
                logger.info("Uploading backup...");

                backupLoadManager.uploadBackup(processedBackupStream, backupProperties, taskId);
                if (Thread.interrupted()) {
                    tasksManager.updateTaskState(taskId, Task.State.INTERRUPTED);
                    throw new InterruptedException();
                }

                tasksManager.updateTaskState(taskId, Task.State.COMPLETED);
                logger.info("Creating backup completed. Backup properties: {}", backupProperties);
            } catch (IOException ex) {
                logger.error("Error occurred while closing input stream of created backup", ex);
            } catch (RuntimeException ex) {
                logger.error("Error occurred while creating backup. Backup properties: {}", backupProperties, ex);
                errorTasksManager.setError(taskId);
            } catch (InterruptedException ex) {
                logger.error("Task was interrupted. Task ID: {}", taskId);
            } finally {
                logger.debug("Unlocking a lock... Task ID: {}", taskId);
                lock.unlock();
                futures.remove(taskId);
            }
        });

        futures.put(taskId, future);
        return future;
    }

    /**
     * Starts backup restoration task.
     * <p>
     * Also sets lock on task. When task is interrupted or completed the lock will be released.
     * If server shutdowns while the lock was held, lock will be released automatically in time described by {@link #lockTimeout}
     * constant.
     *
     * @param taskId           pre-created {@link Task} entity's ID
     * @param backupProperties backup properties of backup saved on storage
     * @param databaseSettings database settings
     * @param logger           the logger
     * @return {@literal Future} of started task
     */
    public Future startRestoreTask(@NotNull Integer taskId, @NotNull BackupProperties backupProperties,
                                   @NotNull DatabaseSettings databaseSettings, @NotNull Logger logger) {
        Objects.requireNonNull(taskId);
        Objects.requireNonNull(backupProperties);
        Objects.requireNonNull(databaseSettings);
        Objects.requireNonNull(logger);

        Future future = tasksStarterExecutorService.submit(() -> {
            logger.debug("Acquiring a lock...");
            SimpleLock lock = jdbcTemplateLockProvider.lock(getNewLockConfiguration(taskId))
                    .orElseThrow(() -> new IllegalStateException("Lock can't be acquired"));
            logger.debug("Lock acquired...");

            tasksManager.updateTaskState(taskId, Task.State.DOWNLOADING);
            logger.info("Downloading backup...");

            try (InputStream downloadedBackup = backupLoadManager.downloadBackup(backupProperties, taskId)) {
                if (Thread.interrupted()) {
                    tasksManager.updateTaskState(taskId, Task.State.INTERRUPTED);
                    throw new InterruptedException();
                }

                tasksManager.updateTaskState(taskId, Task.State.APPLYING_DEPROCESSORS);
                logger.info("Deprocessing backup...");

                InputStream deprocessedBackup = backupProcessorManager.deprocess(downloadedBackup, backupProperties.getProcessors());
                if (Thread.interrupted()) {
                    tasksManager.updateTaskState(taskId, Task.State.INTERRUPTED);
                    throw new InterruptedException();
                }

                tasksManager.updateTaskState(taskId, Task.State.RESTORING);
                logger.info("Restoring backup...");

                databaseBackupManager.restoreBackup(deprocessedBackup, databaseSettings, taskId);
                if (Thread.interrupted()) {
                    tasksManager.updateTaskState(taskId, Task.State.INTERRUPTED);
                    throw new InterruptedException();
                }

                tasksManager.updateTaskState(taskId, Task.State.COMPLETED);
                logger.info("Restoring backup completed. Backup properties: {}", backupProperties);
            } catch (IOException ex) {
                logger.error("Error occurred while closing input stream of downloaded backup", ex);
            } catch (RuntimeException ex) {
                logger.info("Error occurred while restoring backup. Backup properties: {}", backupProperties, ex);
                errorTasksManager.setError(taskId);
            } catch (InterruptedException ex) {
                logger.error("Task was interrupted. Task ID: {}", taskId);
            } finally {
                lock.unlock();
                futures.remove(taskId);
            }
        });

        futures.put(taskId, future);
        return future;
    }

    /**
     * Starts backup deletion task.
     * <p>
     * Also sets lock on task. When task is interrupted or completed the lock will be released.
     * If server shutdowns while the lock was held, lock will be released automatically in time described by {@link #lockTimeout}
     * constant.
     *
     * @param taskId           pre-created {@link Task} entity's ID
     * @param backupProperties backup properties of backup saved on storage
     * @param logger           the logger
     * @return {@literal Future} of started task
     */
    public Future startDeleteTask(@NotNull Integer taskId, @NotNull BackupProperties backupProperties, @NotNull Logger logger) {
        Objects.requireNonNull(taskId);
        Objects.requireNonNull(backupProperties);
        Objects.requireNonNull(logger);

        Future future = tasksStarterExecutorService.submit(() -> {
            logger.debug("Acquiring a lock...");
            SimpleLock lock = jdbcTemplateLockProvider.lock(getNewLockConfiguration(taskId))
                    .orElseThrow(() -> new IllegalStateException("Lock can't be acquired"));
            logger.debug("Lock acquired...");

            try {
                logger.info("Deleting backup started. Backup properties: {}", backupProperties);
                tasksManager.updateTaskState(taskId, Task.State.DELETING);

                backupLoadManager.deleteBackup(backupProperties, taskId);
                if (Thread.interrupted()) {
                    tasksManager.updateTaskState(taskId, Task.State.INTERRUPTED);
                    throw new InterruptedException();
                }

                tasksManager.updateTaskState(taskId, Task.State.COMPLETED);
                logger.info("Deleting backup completed. Backup properties: {}", backupProperties);
            } catch (RuntimeException ex) {
                logger.info("Error occurred while deleting backup. Backup properties: {}",
                        backupProperties, ex);
                errorTasksManager.setError(taskId);
            } catch (InterruptedException ex) {
                logger.error("Task was interrupted. Task ID: {}", taskId);
            } finally {
                lock.unlock();
                futures.remove(taskId);
            }
        });

        futures.put(taskId, future);
        return future;
    }
}
