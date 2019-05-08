package com.blog.controllers;

import com.blog.entities.backup.BackupProperties;
import com.blog.entities.backup.BackupTask;
import com.blog.entities.backup.BackupTaskState;
import com.blog.manager.BackupLoadManager;
import com.blog.manager.BackupTaskManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

@Component
public class BackupStateWatcher {
    private static final Logger logger = LoggerFactory.getLogger(BackupStateWatcher.class);

    private BackupTaskManager backupTaskManager;

    private ExecutorService executorService;

    private BackupLoadManager backupLoadManager;

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

    @Scheduled(fixedRate = 2000)
    @Async
    public void watchStates() {
        logger.info("Watching backup states...");

        for (BackupTask backupTask : backupTaskManager.getBackupTasks()) {
            if (backupTask.isError()) {
                BackupTaskState state = backupTask.getState();

                Integer taskId = backupTask.getId();
                Future<BackupProperties> task = Objects.requireNonNull(backupTaskManager.getTaskFuture(taskId),
                        String.format("No future task with ID %s. Can't abort operation", taskId));

                switch (state) {
                    case UPLOADING: {
                        logger.error("Error occurred while uploading backup. Abort operation...");

                        try {
                            BackupProperties backupProperties = task.get();

                            boolean canceled = task.cancel(true);
                            if (canceled) {
                                Future deletionTask = executorService.submit(
                                        new Runnable() {
                                            @Override
                                            public void run() {
                                                backupLoadManager.deleteBackup(backupProperties);
                                            }
                                        }
                                );
                            } else {
                                logger.error("Can't cancel task with ID {}", taskId);
                            }
                        } catch (InterruptedException | ExecutionException ex) {
                            ex.printStackTrace();
                        }
                    }
                    default: {
                        logger.error("Error occurred while {} operation. Abort operation...", state.toString());

                        backupTaskManager.removeTask(taskId);
                    }
                }
            } else if (backupTask.getState() == BackupTaskState.COMPLETED) {
                Integer taskId = backupTask.getId();

                logger.info("Task with ID {} completed", taskId);

                Future<BackupProperties> task = backupTaskManager.getTaskFuture(taskId);

                try {
                    task.get();
                } catch (InterruptedException | ExecutionException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
}
