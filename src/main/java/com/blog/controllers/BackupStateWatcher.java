package com.blog.controllers;

import com.blog.entities.backup.BackupProperties;
import com.blog.entities.backup.BackupTask;
import com.blog.entities.backup.BackupTaskState;
import com.blog.entities.backup.BackupTaskType;
import com.blog.manager.BackupLoadManager;
import com.blog.manager.BackupPropertiesManager;
import com.blog.manager.BackupTaskManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

@Component
public class BackupStateWatcher {
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

    @Async
    public void cleanCompletedAndInterruptedTasks() {
        for (BackupTask backupTask : backupTaskManager.getBackupTasks()) {
            if (!backupTask.isError()) {
                backupTaskManager.removeTask(backupTask.getId());
            }
        }
    }

    @Scheduled(fixedRate = 2000)
    @Async
    public void watchStates() {
        for (BackupTask backupTask : backupTaskManager.getBackupTasks()) {
            if (backupTask.isError()) {
                BackupTaskState state = backupTask.getState();

                Integer taskId = backupTask.getId();
                Future task = backupTaskManager.getTaskFuture(taskId);

                if (task != null) {
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

                switch (state) {
                    case DOWNLOADING:
                    case APPLYING_DEPROCESSORS:
                    case RESTORING:
                    case DELETING: {
                        logger.error("Error occurred while {} operation. No extra actions required", state.toString());

                        break;
                    }
                    case CREATING:
                    case APPLYING_PROCESSORS: {
                        logger.error("Error occurred while {} operation. Delete backup properties...", state.toString());

                        backupPropertiesManager.deleteById(backupPropertiesId);

                        break;
                    }
                    case UPLOADING: {
                        logger.error("Error occurred while {} operation. Deleting backup from storage...", state);

                        Integer deletionTaskId = backupTaskManager.initNewTask(BackupTaskType.DELETE_BACKUP, backupProperties);
                        Future deletionTask = executorService.submit(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        backupTaskManager.updateTaskState(deletionTaskId, BackupTaskState.DELETING);

                                        try {
                                            backupLoadManager.deleteBackup(backupProperties, deletionTaskId);
                                            backupTaskManager.updateTaskState(deletionTaskId, BackupTaskState.COMPLETED);
                                        } catch (RuntimeException ex) {
                                            logger.error("Can't delete broken backup", ex);
                                        }
                                        backupPropertiesManager.deleteById(backupPropertiesId);
                                    }
                                }
                        );
                        backupTaskManager.addTaskFuture(deletionTaskId, deletionTask);

                        break;
                    }
                    default: {
                        logger.error("Can't handle error occurred while {} operation: Wrong state", state);
                    }
                }
            }
        }
    }
}
