package com.blog.service;

import com.blog.manager.ErrorTasksManager;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.Future;

/**
 * This class allows services to notify about exceptions from additional threads.
 * <p>
 * Many services run additional threads for different work that can produce exceptions, so this is a way to catch these exceptions and
 * mark current task as erroneous to prevent further executing.
 */
@Component
public class ErrorCallbackService {
    private static final Logger logger = LoggerFactory.getLogger(ErrorCallbackService.class);

    private TasksStarterService tasksStarterService;

    private ErrorTasksManager errorTasksManager;

    @Autowired
    public void setTasksStarterService(TasksStarterService tasksStarterService) {
        this.tasksStarterService = tasksStarterService;
    }

    @Autowired
    public void setErrorTasksManager(ErrorTasksManager errorTasksManager) {
        this.errorTasksManager = errorTasksManager;
    }

    /**
     * Thread should manually call this method on exception.
     * <p>
     * This method will not interrupt calling thread, but will interrupt main thread.
     *
     * @param t      exception
     * @param taskId task id
     */
    public void onError(@NotNull Throwable t, @NotNull Integer taskId) {
        logger.error("Error caught: ", t);

        Optional<Future> optionalFuture = tasksStarterService.getFuture(taskId);
        if (!optionalFuture.isPresent()) {
            logger.error("Can't cancel Future of task with ID {}: no such Future instance", taskId);
            return;
        }

        boolean canceled = optionalFuture.get().cancel(true);
        if (!canceled) {
            logger.error("Error canceling Future of task with ID {}", taskId);
            return;
        }
        logger.error("Task canceled. Task ID: {}", taskId);

        errorTasksManager.setError(taskId);
    }
}
