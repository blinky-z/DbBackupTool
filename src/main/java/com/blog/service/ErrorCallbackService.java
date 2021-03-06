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
 * After reporting about an exception current task will be prevented from further executing and marked as interrupted and erroneous.
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
     * Thread should manually call this method when exception occurs.
     * <p>
     * There is no point of calling this method in the main thread, since any exception in the main thread will be caught and the task will
     * be immediately completed with marking task as erroneous.
     * <p>
     * This method will not interrupt the calling thread, but will interrupt the main thread.
     *
     * @param t      an exception
     * @param taskId task id
     * @implNote we set error only if cancellation was successful. If it's not, that means task was already interrupted by user or other error.
     */
    public void onError(@NotNull Throwable t, @NotNull Integer taskId) {
        logger.error("Exception caught. Task ID: {}", taskId, t);

        Optional<Future> optionalFuture = tasksStarterService.getFuture(taskId);
        if (optionalFuture.isEmpty()) {
            logger.error("Can't cancel a Future of the task with ID {}: no such Future instance", taskId);
        } else {
            boolean canceled = optionalFuture.get().cancel(true);
            if (!canceled) {
                logger.error("Error canceling a Future of the task with ID {}", taskId);
            } else {
                errorTasksManager.addErrorTask(taskId);
                logger.info("Task canceled and error status set. Task ID: {}", taskId);
            }
        }
    }
}
