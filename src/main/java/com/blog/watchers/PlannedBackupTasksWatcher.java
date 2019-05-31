package com.blog.watchers;

import com.blog.entities.backup.BackupProperties;
import com.blog.entities.backup.PlannedTask;
import com.blog.entities.backup.Task;
import com.blog.entities.database.DatabaseSettings;
import com.blog.entities.storage.StorageSettings;
import com.blog.manager.*;
import com.blog.service.TasksStarterService;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * This class checks planned tasks and handles them if timer is fired.
 * <p>
 * Task timer resets only if fired planned task was completed successfully (i.e. without errors).
 */
@Component
class PlannedBackupTasksWatcher {
    private static final Logger logger = LoggerFactory.getLogger(PlannedBackupTasksWatcher.class);
    private static final Pageable page = PageRequest.of(0, 10);
    private TasksManager tasksManager;
    private ErrorTasksManager errorTasksManager;
    private PlannedTasksManager plannedTasksManager;
    private DatabaseSettingsManager databaseSettingsManager;
    private StorageSettingsManager storageSettingsManager;
    private BackupPropertiesManager backupPropertiesManager;
    private TasksStarterService tasksStarterService;
    private DataSource dataSource;

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
    public void setStorageSettingsManager(StorageSettingsManager storageSettingsManager) {
        this.storageSettingsManager = storageSettingsManager;
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
    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * This function reverts planned task turning all related tasks into error state.
     *
     * @param plannedTask planned task entity
     */
    private void revertPlannedBackupTask(@NotNull PlannedTask plannedTask) {
        Objects.requireNonNull(plannedTask);

        Integer plannedTaskId = plannedTask.getId();
        List<Integer> executingTasks = plannedTask.getExecutingTasks();
        for (Integer executingTaskId : executingTasks) {
            Optional<Task> optionalBackupTask = tasksManager.findById(executingTaskId);
            if (!optionalBackupTask.isPresent()) {
                logger.error("No such task with ID {} that relates to planned task with ID {}. Skip reverting of this task",
                        executingTaskId, plannedTaskId);
                continue;
            }

            // TODO: сделать остановку всех Future
            // проблема в том, что треды могут находиться в разных серверах, и я не могу из этого треда прервать тред, работающий
            // в другом сервере

            Task task = optionalBackupTask.get();

            errorTasksManager.setError(task.getId());
        }
    }

    /**
     * This watcher wakes up everytime 30 seconds passed from last completion, checks currently executing planned tasks and watches their
     * progress.
     * <p>
     * The watcher handles at most 10 tasks as described by the {@link #page} instance of {@literal Pageable}.
     * <p>
     * When retrieving planned tasks from database pessimistic lock is set. It allows safely run more than one copy of program, as no other
     * watcher can handle already being handled planned tasks.
     * <p>
     * If the server shutdown while rows was locked, transaction will be rolled back and lock released, so these entities can be picked
     * up by the other running server.
     * <p>
     * This watcher does not correlate with {@link #watchPlannedTasks()} watcher and thread can't be blocked when updating entity,
     * because the watchers locks rows of different types.
     *
     * <ol>
     * <li> If all tasks related to current planned task completed successfully, then timer resets and planned task turns into
     * {@link PlannedTask.State#WAITING} state, so the planned task will be waiting for next timer firing.
     * firing.</li>
     * <li> If at least one task relates to current planned task is erroneous, then watcher cancels and reverts all related tasks,
     * the planned task turns into {@link PlannedTask.State#WAITING} state causing it to be picked up by {@link #watchPlannedTasks()}
     * watcher when the one will be started next time.</li>
     * </ol>
     * <p>
     *
     * @throws SQLException if error occurs when working with connection
     */
    @Scheduled(fixedDelay = 30 * 1000)
    void watchExecutingPlannedTasks() throws SQLException {
        Connection connection = dataSource.getConnection();
        connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        connection.setAutoCommit(false);

        for (PlannedTask plannedTask : plannedTasksManager.findAllByState(page, PlannedTask.State.EXECUTING)) {
            if (plannedTask.getState() != PlannedTask.State.EXECUTING) {
                continue;
            }

            Integer plannedBackupTaskId = plannedTask.getId();
            List<Integer> executingTasks = plannedTask.getExecutingTasks();

            boolean plannedTaskCompletedSuccessfully = true;
            boolean errorOccurred = false;
            for (Integer executingTaskId : executingTasks) {
                Optional<Task> optionalBackupTask = tasksManager.findById(executingTaskId);
                if (!optionalBackupTask.isPresent()) {
                    logger.error("No such task with ID {} that relates to planned task with ID {}. Skip checking of this task",
                            executingTaskId, plannedBackupTaskId);
                    continue;
                }

                Task task = optionalBackupTask.get();
                if (errorTasksManager.isError(task.getId())) {
                    errorOccurred = true;
                    break;
                }

                if (task.getState() != Task.State.COMPLETED) {
                    plannedTaskCompletedSuccessfully = false;
                    break;
                }
            }

            if (errorOccurred) {
                // revert planned task and turn into waiting state to be handled by planned tasks watcher further
                revertPlannedBackupTask(plannedTask);
                plannedTasksManager.updateState(plannedBackupTaskId, PlannedTask.State.WAITING);
            } else if (plannedTaskCompletedSuccessfully) {
                // reset timer
                plannedTasksManager.updateLastStartedTimeWithNow(plannedBackupTaskId);
                plannedTasksManager.updateState(plannedBackupTaskId, PlannedTask.State.WAITING);
            }
        }

        connection.commit();
        connection.close();
    }

    /**
     * This watcher wakes up everytime 1 minute passed from last completion, checks waiting planned tasks and starts them if timer is fired.
     * <p>
     * The watcher handles at most 10 tasks as described by the {@link #page} instance of {@literal Pageable}.
     * <p>
     * When retrieving planned tasks from database pessimistic lock is set. It allows safely run more than one copy of program, as no other
     * watcher can handle already being handled planned tasks.
     * <p>
     * If the server shutdown while rows was locked, transaction will be rolled back and lock released, so these entities can be picked
     * up by the other running server.
     * <p>
     * This watcher does not correlate with {@link #watchExecutingPlannedTasks()} watcher and thread can't be blocked when updating entity,
     * because the watchers locks rows of different types.
     * <p>
     * When handling planned task, handler tasks started and list of IDs of these tasks is saved into the same {@link PlannedTask} entity.
     * Considering this, we are able to watch progress of every executing planned task: if any error occurred or all tasks
     * completed successfully.
     *
     * @throws SQLException if error occurs when working with connection
     * @implNote Only tasks in state {@link PlannedTask.State#WAITING} are handled. Once task is started it turns in state
     * {@link PlannedTask.State#EXECUTING} to prevent being handled again.
     * <p>
     * Watcher starts retrieving erroneous tasks from page 0 with limit of 10 as described by the {@link Pageable} instance -
     * {@link #page}.
     * If the first 10 tasks are locked (i.e. acquired by the other watcher) these tasks will be skipped and next page will be retrieved
     * and so on.
     * @see #watchExecutingPlannedTasks()
     */
    @Scheduled(fixedDelay = 60 * 1000)
    void watchPlannedTasks() throws SQLException {
        Connection connection = dataSource.getConnection();
        connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        connection.setAutoCommit(false);

        for (PlannedTask plannedTask : plannedTasksManager.findAllByState(page, PlannedTask.State.WAITING)) {
            LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
            LocalDateTime nextTaskTimeFireTime = plannedTask.getLastStartedTime().plus(plannedTask.getInterval());

            if (now.isBefore(nextTaskTimeFireTime)) {
                continue;
            }

            logger.info("Starting handling of fired planned backup task... Planned backup task info: {}", plannedTask);

            Integer plannedBackupTaskId = plannedTask.getId();

            plannedTasksManager.updateState(plannedBackupTaskId, PlannedTask.State.EXECUTING);

            String databaseSettingsName = plannedTask.getDatabaseSettingsName();
            DatabaseSettings databaseSettings = databaseSettingsManager.getById(databaseSettingsName).orElseThrow(() ->
                    new RuntimeException(String.format("Can't handle planned task: no database settings with name %s",
                            databaseSettingsName)));
            String databaseName = databaseSettings.getName();

            List<Integer> startedTasks = new ArrayList<>();
            for (String storageSettingsName : plannedTask.getStorageSettingsNameList()) {
                StorageSettings storageSettings = storageSettingsManager.getById(storageSettingsName).orElseThrow(() ->
                        new RuntimeException(String.format("Can't handle planned task: no storage settings with name %s",
                                storageSettingsName)));


                BackupProperties backupProperties = backupPropertiesManager.initNewBackupProperties(
                        storageSettings, plannedTask.getProcessors(), databaseName);

                Integer taskId = tasksManager.initNewTask(Task.Type.CREATE_BACKUP, Task.RunType.INTERNAL,
                        backupProperties);
                tasksStarterService.startBackupTask(taskId, backupProperties, databaseSettings, logger);
                startedTasks.add(taskId);
            }

            plannedTasksManager.setExecutingTasks(plannedBackupTaskId, startedTasks);

            logger.info("Planned backup task started. Task ID: {}", plannedBackupTaskId);
        }

        connection.commit();
        connection.close();
    }
}
