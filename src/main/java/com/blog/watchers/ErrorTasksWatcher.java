package com.blog.watchers;

import com.blog.entities.task.ErrorTask;
import com.blog.entities.task.Task;
import com.blog.manager.ErrorTasksManager;
import com.blog.manager.TasksManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * This class scans for erroneous tasks and handles them depending on their state.
 */
@Component
class ErrorTasksWatcher {
    private static final Logger logger = LoggerFactory.getLogger(ErrorTasksWatcher.class);

    private static final Integer nRows = 10;

    private TasksManager tasksManager;

    private ErrorTasksManager errorTasksManager;

    @Autowired
    public void setTasksManager(TasksManager tasksManager) {
        this.tasksManager = tasksManager;
    }

    @Autowired
    public void setErrorTasksManager(ErrorTasksManager errorTasksManager) {
        this.errorTasksManager = errorTasksManager;
    }

    /**
     * This watcher wakes up every time 1 minute passed from the last completion, checks backup states periodically and handles erroneous
     * tasks if any.
     * <p>
     * The watcher handles at most N tasks as described by {@link #nRows} constant and skips already locked tasks.
     * <p>
     * When retrieving error tasks from database pessimistic lock is set. It allows safely run more than one copy of program, as no other
     * watcher can pick up already being handled error tasks.
     * <p>
     * If the server shutdowns while rows was locked, transaction will be rolled back and lock released, so these entities can be picked
     * up by the other running server.
     */
    @Scheduled(fixedDelay = 60 * 1000)
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRES_NEW)
    void watchErrorTasks() {
        for (ErrorTask errorTask : errorTasksManager.findFirstN(nRows)) {
            if (!errorTask.isErrorHandled()) {
                Integer backupTaskId = errorTask.getTaskId();

                Optional<Task> optionalTask = tasksManager.findById(backupTaskId);
                if (!optionalTask.isPresent()) {
                    logger.info("Can't handle erroneous task: no corresponding backup task entity. Backup task ID: {}", backupTaskId);
                    continue;
                }

                tasksManager.revertTask(optionalTask.get());

                errorTasksManager.setErrorHandled(backupTaskId);
            }
        }
    }
}
