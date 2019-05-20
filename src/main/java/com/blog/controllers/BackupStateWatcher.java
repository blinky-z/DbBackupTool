package com.blog.controllers;

import com.blog.entities.backup.BackupProperties;
import com.blog.entities.backup.BackupTask;
import com.blog.manager.BackupLoadManager;
import com.blog.manager.BackupPropertiesManager;
import com.blog.manager.BackupTaskManager;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * This class checks completed, interrupted or erroneous tasks and handles them depending on state.
 * <p>
 * All task states are persisted in database, so even after server fail they will be available for handling.
 * Backup tasks are first checked on server startup and then periodically.
 * <p>
 * Successful tasks are not removing after completing, but will be removed only on next server startup.
 */
@Component
@EnableScheduling
class BackupStateWatcher {
    private static final Logger logger = LoggerFactory.getLogger(BackupStateWatcher.class);

    private BackupTaskManager backupTaskManager;

    private ExecutorService executorService;

    private BackupLoadManager backupLoadManager;

    private BackupPropertiesManager backupPropertiesManager;

    @Autowired
    public void setBackupTaskManager(BackupTaskManager backupTaskManager) {
        this.backupTaskManager = backupTaskManager;
    }

    @Autowired
    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    @Autowired
    public void setBackupLoadManager(BackupLoadManager backupLoadManager) {
        this.backupLoadManager = backupLoadManager;
    }


    @Autowired
    public void setBackupPropertiesManager(BackupPropertiesManager backupPropertiesManager) {
        this.backupPropertiesManager = backupPropertiesManager;
    }

    private void deleteUncompletedBackup(@NotNull BackupProperties backupProperties) {
        Objects.requireNonNull(backupProperties);

        Integer deletionTaskId = backupTaskManager.initNewTask(BackupTask.Type.DELETE_BACKUP, backupProperties);
        Future deletionTask = executorService.submit(
                new Runnable() {
                    @Override
                    public void run() {
                        backupTaskManager.updateTaskState(deletionTaskId, BackupTask.State.DELETING);

                        try {
                            backupPropertiesManager.deleteById(backupProperties.getId());
                            backupLoadManager.deleteBackup(backupProperties, deletionTaskId);
                            backupTaskManager.updateTaskState(deletionTaskId, BackupTask.State.COMPLETED);

                            // we remove task right after completing deleting, since it's not user's task
                            backupTaskManager.removeTask(deletionTaskId);
                        } catch (RuntimeException ex) {
                            logger.error("Error occurred while deleting broken backup", ex);
                            backupTaskManager.setError(deletionTaskId);
                        }
                    }
                }
        );
        backupTaskManager.addTaskFuture(deletionTaskId, deletionTask);
    }

    private void handleBrokenBackupState(@NotNull BackupTask.State state, @NotNull BackupProperties backupProperties) {
        Objects.requireNonNull(state);
        Objects.requireNonNull(backupProperties);

        switch (state) {
            case DOWNLOADING:
            case APPLYING_DEPROCESSORS:
            case RESTORING:
            case DELETING: {
                logger.error("Handling broken operation. Operation: {}. No extra actions required", state.toString());

                break;
            }
            case CREATING:
            case APPLYING_PROCESSORS: {
                logger.error("Handling broken operation. Operation: {}. Delete backup properties...", state.toString());

                backupPropertiesManager.deleteById(backupProperties.getId());

                break;
            }
            case UPLOADING: {
                logger.error("Handling broken operation. Operation: {}. Deleting backup from storage...", state);

                deleteUncompletedBackup(backupProperties);

                break;
            }
            default: {
                logger.error("Can't handle broken operation {}: Unknown state", state);
            }
        }
    }

    /**
     * This function starts once on server startup and removes completed and interrupted tasks from database
     * <p>
     * Erroneous tasks will be handled by daemon function {@link #watchErrorTasks()}
     */
    void handleCompletedAndInterruptedTasks() {
        for (BackupTask backupTask : backupTaskManager.getBackupTasks()) {
            if (!backupTask.isError()) {
                backupTaskManager.removeTask(backupTask.getId());

                BackupTask.State state = backupTask.getState();

                // handle interrupted task
                if (state != BackupTask.State.COMPLETED) {
                    Integer backupPropertiesId = backupTask.getBackupPropertiesId();
                    BackupProperties backupProperties = backupPropertiesManager.findById(backupPropertiesId).
                            orElseThrow(() -> new RuntimeException(String.format(
                                    "Can't handle interrupted operation %s: missing backup properties with ID %s",
                                    state, backupPropertiesId)));

                    handleBrokenBackupState(backupTask.getState(), backupProperties);
                }
            }
        }
    }

    /**
     * This function checks backup states periodically and handles erroneous tasks.
     */
    @Scheduled(fixedRate = 2000)
    void watchErrorTasks() {
        for (BackupTask backupTask : backupTaskManager.getBackupTasks()) {
            if (backupTask.isError()) {
                BackupTask.State state = backupTask.getState();

                Integer taskId = backupTask.getId();
                Optional<Future> optionalTask = backupTaskManager.getTaskFuture(taskId);

                if (optionalTask.isPresent()) {
                    Future task = optionalTask.get();

                    boolean canceled = task.cancel(true);
                    if (!canceled) {
                        logger.error("Error canceling future task with ID {}. Probably task already finished", taskId);
                    }
                } else {
                    logger.error(
                            "No future task with ID {}. Can't abort operation. Probably server shutdown unexpectedly and all tasks has been lost",
                            taskId);
                }

                backupTaskManager.removeTask(taskId);

                Integer backupPropertiesId = backupTask.getBackupPropertiesId();
                BackupProperties backupProperties = backupPropertiesManager.findById(backupPropertiesId).
                        orElseThrow(() -> new RuntimeException(String.format(
                                "Can't handle error occurred while %s operation: missing backup properties with ID %s",
                                state, backupPropertiesId)));

                handleBrokenBackupState(backupTask.getState(), backupProperties);
            }
        }
    }
}
