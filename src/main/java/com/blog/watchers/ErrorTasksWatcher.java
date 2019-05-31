package com.blog.watchers;

import com.blog.entities.backup.ErrorTask;
import com.blog.entities.backup.Task;
import com.blog.manager.ErrorTasksManager;
import com.blog.manager.TasksManager;
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
import java.util.Optional;

/**
 * This class scans for erroneous tasks and handles them depending on their state.
 */
@Component
class ErrorTasksWatcher {
    private static final Logger logger = LoggerFactory.getLogger(ErrorTasksWatcher.class);

    private static final Pageable page = PageRequest.of(0, 10);

    private TasksManager tasksManager;

    private ErrorTasksManager errorTasksManager;

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
    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * This watcher wakes up everytime 1 minute passed from last completion, checks backup states periodically and handles erroneous tasks
     * if such exists.
     * <p>
     * The watcher handles at most 10 tasks as described by the {@link #page} instance of {@literal Pageable}.
     * When retrieving error tasks from database pessimistic lock is set. It allows safely run more than one copy of program, as no other
     * watcher can handle already being handled error tasks again.
     * <p>
     * If the server shutdown while rows was locked, transaction will be rolled back and lock released, so these entities can be picked
     * up by the other running server.
     * <p>
     * When handling erroneous task, corresponding {@literal Future} is canceled and then task is reverted using
     * {@link TasksManager#revertTask(Task)} method.
     */
    @Scheduled(fixedDelay = 60 * 1000)
    void watchErrorTasks() throws SQLException {
        Connection connection = dataSource.getConnection();
        connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        connection.setAutoCommit(false);

        for (ErrorTask errorTask : errorTasksManager.findAll(page)) {
            if (!errorTask.isErrorHandled()) {
                Integer backupTaskId = errorTask.getTaskId();

                Optional<Task> optionalBackupTask = tasksManager.findById(backupTaskId);
                if (!optionalBackupTask.isPresent()) {
                    logger.info("Can't handle erroneous task: no corresponding backup task entity. Backup task ID: {}", backupTaskId);
                    continue;
                }

                tasksManager.revertTask(optionalBackupTask.get());

                errorTasksManager.setErrorHandled(backupTaskId);
            }
        }

        connection.commit();
        connection.close();
    }
}
