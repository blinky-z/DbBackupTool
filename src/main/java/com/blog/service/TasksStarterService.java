package com.blog.service;

import com.blog.entities.backup.BackupProperties;
import com.blog.entities.database.DatabaseSettings;
import com.blog.entities.task.Task;
import com.blog.manager.*;
import com.blog.service.processor.ProcessorType;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
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
    private final ConcurrentHashMap<Integer, Future> futures = new ConcurrentHashMap<>();

    private ExecutorService tasksStarterExecutorService;
    private TasksManager tasksManager;
    private DatabaseBackupManager databaseBackupManager;
    private BackupProcessorManager backupProcessorManager;
    private BackupLoadManager backupLoadManager;
    private ErrorTasksManager errorTasksManager;

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
     *
     * @param taskId           pre-created {@link Task} entity's ID
     * @param backupProperties pre-created backup properties
     * @param databaseSettings database settings
     * @param logger           the logger
     * @return {@literal Future} of started task
     * @see BackupPropertiesManager#initNewBackupProperties(List, List, String)
     * @see TasksManager#initNewTask(Task.Type, Task.RunType, Integer)
     */
    public Future startBackupTask(@NotNull Integer taskId, @NotNull BackupProperties backupProperties,
                                  @NotNull DatabaseSettings databaseSettings, @NotNull Logger logger) {
        Objects.requireNonNull(taskId);
        Objects.requireNonNull(backupProperties);
        Objects.requireNonNull(databaseSettings);
        Objects.requireNonNull(logger);

        Future future = tasksStarterExecutorService.submit(() -> {
            tasksManager.updateTaskState(taskId, Task.State.CREATING);
            logger.info("Creating backup...");

            try (InputStream backupStream = databaseBackupManager.createBackup(databaseSettings, taskId)) {
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }

                tasksManager.updateTaskState(taskId, Task.State.APPLYING_PROCESSORS);
                List<ProcessorType> processors = backupProperties.getProcessors();
                logger.info("Applying processors on created backup. Processors: {}", processors);

                try (InputStream processedBackupStream = backupProcessorManager.process(backupStream, processors)) {
                    if (Thread.interrupted()) {
                        throw new InterruptedException();
                    }

                    tasksManager.updateTaskState(taskId, Task.State.UPLOADING);
                    logger.info("Uploading backup...");

                    backupLoadManager.uploadBackup(processedBackupStream, backupProperties, taskId);
                    if (Thread.interrupted()) {
                        throw new InterruptedException();
                    }

                    tasksManager.updateTaskState(taskId, Task.State.COMPLETED);
                    logger.info("Creating backup completed. Backup properties: {}", backupProperties);
                }
            } catch (IOException ex) {
                logger.error("Error occurred while closing input stream of created backup", ex);
            } catch (RuntimeException ex) {
                logger.error("Error occurred while creating backup. Backup properties: {}", backupProperties, ex);
                errorTasksManager.setError(taskId);
            } catch (InterruptedException ex) {
                tasksManager.setInterrupted(taskId);
                logger.error("Backup creating task was interrupted. Task ID: {}", taskId);
            } finally {
                futures.remove(taskId);
            }
        });

        futures.put(taskId, future);
        return future;
    }

    /**
     * Starts backup restoration task.
     *
     * @param taskId              pre-created {@link Task} entity's ID
     * @param backupProperties    backup properties of backup saved on storage
     * @param storageSettingsName storage settings name
     * @param databaseSettings    database settings
     * @param logger              the logger
     * @return {@literal Future} of started task
     */
    public Future startRestoreTask(@NotNull Integer taskId, @NotNull BackupProperties backupProperties, @NotNull String storageSettingsName,
                                   @NotNull DatabaseSettings databaseSettings, @NotNull Logger logger) {
        Objects.requireNonNull(taskId);
        Objects.requireNonNull(backupProperties);
        Objects.requireNonNull(storageSettingsName);
        Objects.requireNonNull(databaseSettings);
        Objects.requireNonNull(logger);

        Future future = tasksStarterExecutorService.submit(() -> {
            tasksManager.updateTaskState(taskId, Task.State.DOWNLOADING);
            logger.info("Downloading backup...");

            try (InputStream downloadedBackup =
                         backupLoadManager.downloadBackup(backupProperties.getBackupName(), storageSettingsName, taskId)) {
                if (Thread.interrupted() || downloadedBackup == null) {
                    throw new InterruptedException();
                }

                tasksManager.updateTaskState(taskId, Task.State.APPLYING_DEPROCESSORS);
                logger.info("Deprocessing backup...");

                try (InputStream deprocessedBackup = backupProcessorManager.deprocess(downloadedBackup, backupProperties.getProcessors())) {
                    if (Thread.interrupted()) {
                        throw new InterruptedException();
                    }

                    tasksManager.updateTaskState(taskId, Task.State.RESTORING);
                    logger.info("Restoring backup...");

                    databaseBackupManager.restoreBackup(deprocessedBackup, databaseSettings, taskId);
                    if (Thread.interrupted()) {
                        throw new InterruptedException();
                    }

                    tasksManager.updateTaskState(taskId, Task.State.COMPLETED);
                    logger.info("Restoring backup completed. Backup properties: {}", backupProperties);
                }
            } catch (IOException ex) {
                logger.error("Error occurred while closing input stream of downloaded backup", ex);
            } catch (RuntimeException ex) {
                logger.info("Error occurred while restoring backup. Backup properties: {}", backupProperties, ex);
                errorTasksManager.setError(taskId);
            } catch (InterruptedException ex) {
                tasksManager.setInterrupted(taskId);
                logger.error("Task was interrupted. Task ID: {}", taskId);
            } finally {
                futures.remove(taskId);
            }
        });

        futures.put(taskId, future);
        return future;
    }

    /**
     * Starts backup deletion task.
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
            try {
                logger.info("Deleting backup started. Backup properties: {}", backupProperties);
                tasksManager.updateTaskState(taskId, Task.State.DELETING);

                backupLoadManager.deleteBackup(backupProperties, taskId);
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }

                tasksManager.updateTaskState(taskId, Task.State.COMPLETED);
                logger.info("Deleting backup completed. Backup properties: {}", backupProperties);
            } catch (RuntimeException ex) {
                logger.error("Error occurred while deleting backup. Backup properties: {}", backupProperties, ex);
                errorTasksManager.setError(taskId);
            } catch (InterruptedException ex) {
                tasksManager.setInterrupted(taskId);
                logger.error("Task was interrupted. Task ID: {}", taskId);
            } finally {
                futures.remove(taskId);
            }
        });

        futures.put(taskId, future);
        return future;
    }
}
