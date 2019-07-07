package com.blog.service;

import com.blog.entities.backup.BackupProperties;
import com.blog.entities.database.DatabaseSettings;
import com.blog.entities.task.Task;
import com.blog.manager.*;
import com.blog.service.processor.ProcessorType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * This class provides API to start tasks: creation, restoration and deletion of backups.
 */
@Component
@Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRES_NEW)
public class TasksStarterService {
    private static final Logger logger = LoggerFactory.getLogger(TasksStarterService.class);
    private final ConcurrentHashMap<Integer, Future> futures = new ConcurrentHashMap<>();

    private ExecutorService tasksStarterExecutorService;
    private TasksManager tasksManager;
    private DatabaseBackupManager databaseBackupManager;
    private BackupProcessorManager backupProcessorManager;
    private BackupLoadManager backupLoadManager;
    private BackupPropertiesManager backupPropertiesManager;
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
    public void setBackupPropertiesManager(BackupPropertiesManager backupPropertiesManager) {
        this.backupPropertiesManager = backupPropertiesManager;
    }

    @Autowired
    public void setErrorTasksManager(ErrorTasksManager errorTasksManager) {
        this.errorTasksManager = errorTasksManager;
    }

    /**
     * Returns the {@literal Future} related to specified {@link Task}.
     * <p>
     * Usually you want to get {@literal Future} to cancel task.
     *
     * @param taskId {@link Task} entity's ID
     * @return the Future or {@literal Optional#empty()} if none found
     */
    public Optional<Future> getFuture(@NotNull Integer taskId) {
        return Optional.ofNullable(futures.get(taskId));
    }

    /**
     * Starts backup creation task.
     *
     * @param databaseSettings database settings
     * @return the {@link Task} entity of started task
     * @see BackupPropertiesManager#initNewBackupProperties(List, List, String)
     */
    public Task startBackupTask(@NotNull Task.RunType runType, @NotNull List<String> storageSettingsNameList, @Nullable List<ProcessorType> processors,
                                @NotNull DatabaseSettings databaseSettings) {
        Objects.requireNonNull(runType);
        Objects.requireNonNull(storageSettingsNameList);
        Objects.requireNonNull(processors);
        Objects.requireNonNull(databaseSettings);

        BackupProperties backupProperties =
                backupPropertiesManager.initNewBackupProperties(storageSettingsNameList, processors, databaseSettings.getName());
        Task task = tasksManager.initNewTask(Task.Type.CREATE_BACKUP, runType, backupProperties.getId());
        Integer taskId = task.getId();

        Future future = tasksStarterExecutorService.submit(() -> {
            tasksManager.updateTaskState(taskId, Task.State.CREATING);
            logger.info("Creating backup...");

            try (InputStream backupStream = databaseBackupManager.createBackup(databaseSettings, taskId)) {
                if (Thread.interrupted()) {
                    throw new InterruptedException();
                }

                tasksManager.updateTaskState(taskId, Task.State.APPLYING_PROCESSORS);
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
                errorTasksManager.addErrorTask(taskId);
            } catch (InterruptedException ex) {
                tasksManager.setInterrupted(taskId);
                logger.error("Backup creating task was interrupted. Task ID: {}", taskId);
            } finally {
                futures.remove(taskId);
            }
        });

        futures.put(taskId, future);
        return task;
    }

    /**
     * Starts backup restoration task.
     *
     * @param backupProperties    backup properties of backup saved on storage
     * @param storageSettingsName storage settings name
     * @param databaseSettings    database settings
     * @return the {@link Task} entity of started task
     */
    public Task startRestoreTask(@NotNull Task.RunType runType, @NotNull BackupProperties backupProperties, @NotNull String storageSettingsName,
                                 @NotNull DatabaseSettings databaseSettings) {
        Objects.requireNonNull(runType);
        Objects.requireNonNull(backupProperties);
        Objects.requireNonNull(storageSettingsName);
        Objects.requireNonNull(databaseSettings);

        Task task = tasksManager.initNewTask(Task.Type.RESTORE_BACKUP, runType, backupProperties.getId());
        Integer taskId = task.getId();

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
                errorTasksManager.addErrorTask(taskId);
            } catch (InterruptedException ex) {
                tasksManager.setInterrupted(taskId);
                logger.error("Task was interrupted. Task ID: {}", taskId);
            } finally {
                futures.remove(taskId);
            }
        });

        futures.put(taskId, future);
        return task;
    }

    /**
     * Starts backup deletion task.
     * <p>
     * This method does not delete the related entity of backup.
     *
     * @param backupProperties backup properties of backup saved on storage
     * @return the {@link Task} entity of started task
     */
    public Task startDeleteTask(@NotNull Task.RunType runType, @NotNull BackupProperties backupProperties) {
        Objects.requireNonNull(runType);
        Objects.requireNonNull(backupProperties);

        Task task = tasksManager.initNewTask(Task.Type.DELETE_BACKUP, runType, backupProperties.getId());
        Integer taskId = task.getId();

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
                errorTasksManager.addErrorTask(taskId);
            } catch (InterruptedException ex) {
                tasksManager.setInterrupted(taskId);
                logger.error("Task was interrupted. Task ID: {}", taskId);
            } finally {
                futures.remove(taskId);
            }
        });

        futures.put(taskId, future);
        return task;
    }
}
