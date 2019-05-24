package com.blog.controllers;

import com.blog.entities.backup.BackupProperties;
import com.blog.entities.backup.BackupTask;
import com.blog.entities.backup.BackupTaskType;
import com.blog.entities.backup.PlannedBackupTask;
import com.blog.entities.database.DatabaseSettings;
import com.blog.entities.storage.StorageSettings;
import com.blog.manager.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Component
class PlannedBackupTasksWatcher {
    private static final Logger logger = LoggerFactory.getLogger(PlannedBackupTasksWatcher.class);

    private static final ExecutorService executorService = Executors.newFixedThreadPool(10);

    private BackupTaskManager backupTaskManager;

    private PlannedBackupTasksManager plannedBackupTasksManager;

    private DatabaseBackupManager databaseBackupManager;

    private BackupLoadManager backupLoadManager;

    private BackupProcessorManager backupProcessorManager;

    private DatabaseSettingsManager databaseSettingsManager;

    private StorageSettingsManager storageSettingsManager;

    @Autowired
    public void setBackupTaskManager(BackupTaskManager backupTaskManager) {
        this.backupTaskManager = backupTaskManager;
    }

    @Autowired
    public void setPlannedBackupTasksManager(PlannedBackupTasksManager plannedBackupTasksManager) {
        this.plannedBackupTasksManager = plannedBackupTasksManager;
    }

    @Autowired
    public void setDatabaseBackupManager(DatabaseBackupManager databaseBackupManager) {
        this.databaseBackupManager = databaseBackupManager;
    }

    @Autowired
    public void setBackupLoadManager(BackupLoadManager backupLoadManager) {
        this.backupLoadManager = backupLoadManager;
    }

    @Autowired
    public void setBackupProcessorManager(BackupProcessorManager backupProcessorManager) {
        this.backupProcessorManager = backupProcessorManager;
    }

    @Autowired
    public void setDatabaseSettingsManager(DatabaseSettingsManager databaseSettingsManager) {
        this.databaseSettingsManager = databaseSettingsManager;
    }

    @Autowired
    public void setStorageSettingsManager(StorageSettingsManager storageSettingsManager) {
        this.storageSettingsManager = storageSettingsManager;
    }

    @Scheduled(fixedRate = 5 * 1000)
    void watchPlannedTasks() {
        for (PlannedBackupTask plannedBackupTask : plannedBackupTasksManager.findAll()) {
            Instant now = Instant.now();
            Instant nextTaskTimeFireTime = plannedBackupTask.getLastStartedTime().plus(plannedBackupTask.getInterval());

            if (now.isBefore(nextTaskTimeFireTime)) {
                return;
            }

            logger.info("Starting handling of fired planned backup task... Planned backup task info: {}", plannedBackupTask);

            plannedBackupTasksManager.updateLastStartedTimeWithCurrent(plannedBackupTask.getId());

            String databaseSettingsName = plannedBackupTask.getDatabaseSettingsName();
            DatabaseSettings databaseSettings = databaseSettingsManager.getById(databaseSettingsName).orElseThrow(() ->
                    new RuntimeException(String.format("Can't retrieve database settings. Error: no database settings with name %s",
                            databaseSettingsName)));
            String databaseName = databaseSettings.getName();
            List<String> processors = plannedBackupTask.getProcessors();

            for (String storageSettingsName : plannedBackupTask.getStorageSettingsNameList()) {
                StorageSettings storageSettings = storageSettingsManager.getById(storageSettingsName).orElseThrow(() ->
                        new RuntimeException(String.format("Can't retrieve storage settings. Error: no storage settings with name %s",
                                storageSettingsName)));

                BackupProperties backupProperties = backupLoadManager.initNewBackupProperties(
                        storageSettings, processors, databaseName);

                Integer taskId = backupTaskManager.initNewTask(BackupTaskType.CREATE_BACKUP, BackupTask.RunType.INTERNAL,
                        backupProperties);
                Future task = executorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            backupTaskManager.updateTaskState(taskId, BackupTask.State.CREATING);
                            InputStream backupStream = databaseBackupManager.createBackup(databaseSettings, taskId);

                            backupTaskManager.updateTaskState(taskId, BackupTask.State.APPLYING_PROCESSORS);
                            InputStream processedBackupStream = backupProcessorManager.process(backupStream, processors);

                            backupTaskManager.updateTaskState(taskId, BackupTask.State.UPLOADING);
                            backupLoadManager.uploadBackup(processedBackupStream, backupProperties, taskId);

                            backupTaskManager.updateTaskState(taskId, BackupTask.State.COMPLETED);
                        } catch (RuntimeException ex) {
                            backupTaskManager.setError(taskId);
                        }
                    }
                });
                backupTaskManager.addTaskFuture(taskId, task);
            }

            if (plannedBackupTask.getType() == PlannedBackupTask.Type.ONCE) {
                plannedBackupTasksManager.deleteById(plannedBackupTask.getId());
            }

            logger.info("Planned backup task handled. Task ID: {}", plannedBackupTask.getId());
        }
    }
}
