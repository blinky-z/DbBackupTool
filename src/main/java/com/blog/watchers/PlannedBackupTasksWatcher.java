package com.blog.watchers;

import com.blog.entities.backup.BackupProperties;
import com.blog.entities.database.DatabaseSettings;
import com.blog.entities.task.PlannedTask;
import com.blog.entities.task.Task;
import com.blog.manager.*;
import com.blog.service.TasksStarterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

/**
 * This class checks planned tasks and handles them if timer is fired.
 * <p>
 * Task timer resets only if fired planned task was completed successfully (i.e. without errors).
 */
@Component
class PlannedBackupTasksWatcher {
    private static final Logger logger = LoggerFactory.getLogger(PlannedBackupTasksWatcher.class);
    private static final Integer nRows = 10;
    private TasksManager tasksManager;
    private ErrorTasksManager errorTasksManager;
    private PlannedTasksManager plannedTasksManager;
    private DatabaseSettingsManager databaseSettingsManager;
    private BackupPropertiesManager backupPropertiesManager;
    private TasksStarterService tasksStarterService;
    private CancelTasksManager cancelTasksManager;

    @Autowired
    public void setTasksManager(TasksManager tasksManager) {
        this.tasksManager = tasksManager;
    }

    @Autowired
    public void setErrorTasksManager(ErrorTasksManager errorTasksManager) {
        this.errorTasksManager = errorTasksManager;
    }

    @Autowired
    public void setPlannedTasksManager(PlannedTasksManager plannedTasksManager) {
        this.plannedTasksManager = plannedTasksManager;
    }

    @Autowired
    public void setDatabaseSettingsManager(DatabaseSettingsManager databaseSettingsManager) {
        this.databaseSettingsManager = databaseSettingsManager;
    }

    @Autowired
    public void setBackupPropertiesManager(BackupPropertiesManager backupPropertiesManager) {
        this.backupPropertiesManager = backupPropertiesManager;
    }

    @Autowired
    public void setTasksStarterService(TasksStarterService tasksStarterService) {
        this.tasksStarterService = tasksStarterService;
    }

    @Autowired
    public void setCancelTasksManager(CancelTasksManager cancelTasksManager) {
        this.cancelTasksManager = cancelTasksManager;
    }

    /**
     * This watcher wakes up every time 30 seconds passed from the last completion, checks currently executing planned tasks and watches their
     * progress.
     * <p>
     * The watcher handles at most N tasks as described by {@link #nRows} constant and skips already locked tasks.
     * When retrieving planned tasks from database pessimistic lock is set. It allows safely run more than one copy of program, as no other
     * watcher can handle already being handled planned tasks.
     * <p>
     * If the server shutdowns while rows was locked, transaction will be rolled back and lock released, so these entities can be picked
     * up by the other running server.
     * <p>
     * This watcher does not correlate with {@link #watchPlannedTasks()} watcher and thread can't be blocked when updating entity,
     * because the watchers locks tasks of different types.
     *
     * <ul>
     * <li> If all tasks related to current planned task completed successfully, then timer resets and planned task turns into
     * {@link PlannedTask.State#WAITING} state, so the planned task will be waiting for next timer firing.
     * firing.</li>
     * <li> If at least one task relates to current planned task is erroneous, then watcher marks all handler tasks as erroneous and
     * the planned task turns into {@link PlannedTask.State#WAITING} state causing it to be picked up by {@link #watchPlannedTasks()}
     * watcher.</li>
     * </ul>
     */
    @Scheduled(fixedDelay = 30 * 1000)
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRES_NEW)
    void watchExecutingPlannedTasks() {
        for (PlannedTask plannedTask : plannedTasksManager.findFirstNByStateAndLock(nRows, PlannedTask.State.EXECUTING)) {
            if (plannedTask.getState() != PlannedTask.State.EXECUTING) {
                continue;
            }

            Integer plannedTaskId = plannedTask.getId();

            Integer handlerTaskId = plannedTask.getHandlerTaskId();
            if (handlerTaskId == null) {
                logger.error("ILLEGAL STATE: planned task is executing, but no handler task. Planned task info: {}. Starting the planned task again...",
                        plannedTask);
                plannedTasksManager.updateState(plannedTaskId, PlannedTask.State.WAITING);
                continue;
            }

            Optional<Task> optionalHandlerTask = tasksManager.findById(handlerTaskId);
            if (!optionalHandlerTask.isPresent()) {
                logger.error("No such handler task with ID {}. Planned task info: {}. Starting the planned task again...",
                        handlerTaskId, plannedTask);
                plannedTasksManager.updateState(plannedTaskId, PlannedTask.State.WAITING);
                continue;
            }

            if (errorTasksManager.isError(handlerTaskId)) {
                cancelTasksManager.addTaskToCancel(handlerTaskId);
                plannedTasksManager.updateState(plannedTaskId, PlannedTask.State.WAITING);
                continue;
            }

            if (optionalHandlerTask.get().getState() == Task.State.COMPLETED) {
                // reset timer
                plannedTask.setHandlerTaskId(null);
                plannedTasksManager.updateLastStartedTimeWithNow(plannedTaskId);
                plannedTasksManager.updateState(plannedTaskId, PlannedTask.State.WAITING);
            }
        }
    }

    /**
     * This watcher wakes up every time 1 minute passed from the last completion, checks waiting planned tasks and starts them if timer is fired.
     * <p>
     * The watcher handles at most N tasks as described by {@link #nRows} constant. It skips locked tasks.
     * When retrieving planned tasks from database pessimistic lock is set. It allows safely run more than one copy of program, as no other
     * watcher can handle already being handled planned tasks.
     * <p>
     * If the server shutdowns while rows was locked, transaction will be rolled back and lock released, so these entities can be picked
     * up by the other running server.
     * <p>
     * This watcher does not correlate with {@link #watchExecutingPlannedTasks()} watcher and thread can't be blocked when updating entity,
     * because the watchers locks tasks of different types.
     * <p>
     * When handling planned task, handler tasks started and list of IDs of these tasks is saved into the same {@link PlannedTask} entity.
     * Considering this, we are able to watch progress of every executing planned task: if any error occurred or all tasks
     * completed successfully.
     *
     * @implNote Only tasks in state {@link PlannedTask.State#WAITING} are handled. Once task is started it turns in state
     * {@link PlannedTask.State#EXECUTING} to prevent being handled again.
     * @see #watchExecutingPlannedTasks()
     */
    @Scheduled(fixedDelay = 60 * 1000)
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRES_NEW)
    void watchPlannedTasks() {
        for (PlannedTask plannedTask : plannedTasksManager.findFirstNByStateAndLock(nRows, PlannedTask.State.WAITING)) {
            LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
            LocalDateTime nextTaskTimeFireTime = plannedTask.getLastStartedTime().plus(plannedTask.getInterval());

            if (now.isBefore(nextTaskTimeFireTime)) {
                continue;
            }

            logger.info("Starting handling of fired planned backup task... Planned backup task info: {}", plannedTask);

            Integer plannedBackupTaskId = plannedTask.getId();

            plannedTasksManager.updateState(plannedBackupTaskId, PlannedTask.State.EXECUTING);

            String databaseSettingsName = plannedTask.getDatabaseSettingsName();
            Optional<DatabaseSettings> optionalDatabaseSettings = databaseSettingsManager.findById(databaseSettingsName);
            if (!optionalDatabaseSettings.isPresent()) {
                logger.error("Can't handle planned task: no database settings with name {}. Planned task info: {}",
                        databaseSettingsName, plannedTask);
                continue;
            }

            DatabaseSettings databaseSettings = optionalDatabaseSettings.get();

            BackupProperties backupProperties = backupPropertiesManager.initNewBackupProperties(
                    plannedTask.getStorageSettingsNameList(), plannedTask.getProcessors(), databaseSettings.getName());
            Integer handlerTaskId = tasksManager.initNewTask(Task.Type.CREATE_BACKUP, Task.RunType.INTERNAL, backupProperties.getId());

            tasksStarterService.startBackupTask(handlerTaskId, backupProperties, databaseSettings, logger);

            plannedTask.setHandlerTaskId(handlerTaskId);

            logger.info("Planned backup task started. Planned Task info: {}", plannedTask);
        }
    }
}
