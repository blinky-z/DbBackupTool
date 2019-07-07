package com.blog.watchers;

import com.blog.entities.task.CancelTask;
import com.blog.entities.task.Task;
import com.blog.manager.CancelTasksManager;
import com.blog.manager.TasksManager;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * This class scans for tasks to cancel and tried to cancel them.
 */
@Component
class CancelTasksWatcher {
    private static final Logger logger = LoggerFactory.getLogger(CancelTasksWatcher.class);
    private static final Duration cancelTimeout = Duration.ofMinutes(10);
    private CancelTasksManager cancelTasksManager;
    private TasksStarterService tasksStarterService;
    private TasksManager tasksManager;

    @Autowired
    public void setCancelTasksManager(CancelTasksManager cancelTasksManager) {
        this.cancelTasksManager = cancelTasksManager;
    }

    @Autowired
    public void setTasksStarterService(TasksStarterService tasksStarterService) {
        this.tasksStarterService = tasksStarterService;
    }

    @Autowired
    public void setTasksManager(TasksManager tasksManager) {
        this.tasksManager = tasksManager;
    }

    /**
     * This watcher wakes up every time 10 seconds passed from the last completion, checks if there are any tasks to cancel and tries to
     * cancel each task.
     * <p>
     * Since there are can be working more that one instance of the program, {@literal Future} instance of task can belong to different
     * servers. We can't get access to {@literal Future} if it's not in memory of the server where task cancellation request was accepted.
     * So the purpose of this watcher is to be able cancel tasks that works in the other instance of program. Each server has this watcher
     * checking for available cancellation requests and if any, the watcher tries to cancel corresponding {@literal Future}.
     * If cancellation is successful task will be also reverted.
     * <p>
     * If task cancellation request timeout exceeded, then it means a server that had requested {@literal Future} instances has been
     * shutdown, so all {@literal Future} instances lost and task can't be canceled. In such case task cancellation request will be ignored.
     *
     * @see TasksStarterService#getFuture(Integer)
     * @see TasksManager#revertTask(Task)
     */
    @Scheduled(fixedDelay = 10 * 1000)
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRES_NEW)
    public void watchTasksToCancel() {
        Iterable<CancelTask> cancelTasks = cancelTasksManager.findAll();

        Iterable<Task> tasks = tasksManager.findAllById(StreamSupport.stream(cancelTasks.spliterator(), false)
                .map(CancelTask::getTaskId).collect(Collectors.toList()));
        Map<Integer, Task> tasksAsMap = StreamSupport.stream(tasks.spliterator(), false)
                .collect(Collectors.toMap(Task::getId, Function.identity()));

        List<Integer> taskIdsForDeleting = new ArrayList<>();

        for (CancelTask cancelTask : cancelTasks) {
            Integer taskId = cancelTask.getTaskId();

            Task task = tasksAsMap.get(taskId);
            if (task == null) {
                logger.error("Can't cancel task: no such entity with ID {}", taskId);
                taskIdsForDeleting.add(taskId);
                continue;
            }

            // timeout exceeded, that is server shutdown and lost all Future instances, so task can't be canceled
            if (LocalDateTime.now(ZoneOffset.UTC).isAfter(cancelTask.getPutTime().plus(cancelTimeout))) {
                logger.error("Can't cancel task: timeout exceed. Task ID: {}", taskId);
                taskIdsForDeleting.add(taskId);
                continue;
            }

            tasksStarterService.getFuture(taskId).ifPresent(future -> {
                logger.info("Canceling task with ID {}", taskId);

                boolean canceled = future.cancel(true);
                if (canceled) {
                    try {
                        // give time to properly handle interrupt
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        // should not happen
                    }
                    tasksManager.revertTask(task);
                }

                taskIdsForDeleting.add(taskId);

                logger.info("Task canceled: {}. Task ID: {}", canceled, taskId);
            });
        }

        cancelTasksManager.deleteByTaskIdIn(taskIdsForDeleting);
    }
}
