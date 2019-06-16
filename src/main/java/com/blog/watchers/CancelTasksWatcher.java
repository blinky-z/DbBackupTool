package com.blog.watchers;

import com.blog.entities.task.CancelTask;
import com.blog.entities.task.Task;
import com.blog.manager.CancelTasksManager;
import com.blog.manager.TasksManager;
import com.blog.repositories.TasksRepository;
import com.blog.service.TasksStarterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.concurrent.Future;

/**
 * This class scans for tasks to cancel and cancels them.
 */
@Component
public class CancelTasksWatcher {
    private static final Logger logger = LoggerFactory.getLogger(CancelTasksWatcher.class);
    private static final Duration cancelTimeout = Duration.ofMinutes(10);
    private CancelTasksManager cancelTasksManager;
    private TasksRepository tasksRepository;
    private TasksStarterService tasksStarterService;
    private TasksManager tasksManager;

    @Autowired
    public void setCancelTasksManager(CancelTasksManager cancelTasksManager) {
        this.cancelTasksManager = cancelTasksManager;
    }

    @Autowired
    public void setTasksRepository(TasksRepository tasksRepository) {
        this.tasksRepository = tasksRepository;
    }

    @Autowired
    public void setTasksStarterService(TasksStarterService tasksStarterService) {
        this.tasksStarterService = tasksStarterService;
    }

    @Autowired
    public void setTasksManager(TasksManager tasksManager) {
        this.tasksManager = tasksManager;
    }

    @Scheduled(fixedDelay = 10 * 1000)
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRES_NEW)
    void watchTasksToCancel() {
        for (CancelTask cancelTask : cancelTasksManager.findAll()) {
            Integer taskId = cancelTask.getTaskId();

            Optional<Task> optionalTask = tasksRepository.findById(taskId);
            if (!optionalTask.isPresent()) {
                logger.error("Can't cancel task: no such task with ID {}", taskId);
                cancelTasksManager.deleteByTaskId(taskId);
                continue;
            }

            // timeout exceeded, that is server shutdown and lost all Future instances, so task can't be canceled
            if (LocalDateTime.now(ZoneOffset.UTC).isAfter(cancelTask.getPutTime().plus(cancelTimeout))) {
                logger.error("Can't cancel task: timeout exceed. Task ID: {}", taskId);
                cancelTasksManager.deleteByTaskId(taskId);
                continue;
            }

            Optional<Future> optionalFuture = tasksStarterService.getFuture(taskId);
            if (!optionalFuture.isPresent()) {
                continue;
            }

            logger.info("Canceling task with ID {}", taskId);

            optionalFuture.get().cancel(true);
            tasksManager.revertTask(optionalTask.get());

            cancelTasksManager.deleteByTaskId(taskId);

            logger.info("Task canceled. Task ID: {}", taskId);
        }
    }
}
