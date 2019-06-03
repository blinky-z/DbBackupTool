package com.blog.manager;

import com.blog.entities.backup.BackupProperties;
import com.blog.entities.storage.StorageSettings;
import com.blog.entities.task.Task;
import com.blog.repositories.TasksRepository;
import com.blog.service.TasksStarterService;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * This class provides API to manage tasks.
 */
@Component
@Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRES_NEW)
public class TasksManager {
    private static final Logger logger = LoggerFactory.getLogger(TasksManager.class);

    private static final Task.State initialBackupTaskState = Task.State.PLANNED;

    private TasksStarterService tasksStarterService;

    private TasksRepository tasksRepository;

    private BackupPropertiesManager backupPropertiesManager;

    @Autowired
    public void setBackupPropertiesManager(BackupPropertiesManager backupPropertiesManager) {
        this.backupPropertiesManager = backupPropertiesManager;
    }

    @Autowired
    public void setTasksRepository(TasksRepository tasksRepository) {
        this.tasksRepository = tasksRepository;
    }

    @Autowired
    public void setTasksStarterService(TasksStarterService tasksStarterService) {
        this.tasksStarterService = tasksStarterService;
    }

    /**
     * Creates a new {@link Task} instance.
     *
     * @param taskType         backup task type
     * @param backupProperties backup properties of created or being created backup
     * @return ID of created task
     * @see BackupPropertiesManager#initNewBackupProperties(StorageSettings, List, String)
     */
    public Integer initNewTask(Task.Type taskType, Task.RunType runType, BackupProperties backupProperties) {
        Task task = new Task.Builder()
                .withType(taskType)
                .withRunType(runType)
                .withBackupPropertiesId(backupProperties.getId())
                .withState(initialBackupTaskState)
                .withDate(LocalDateTime.now(ZoneOffset.UTC))
                .build();

        task = tasksRepository.save(task);

        return task.getId();
    }

    /**
     * Updates task state.
     *
     * @param id    ID of task
     * @param state new state to set
     */
    public void updateTaskState(Integer id, Task.State state) {
        Task task = tasksRepository.findById(id).orElseThrow(RuntimeException::new);
        task.setState(state);
    }

    /**
     * This function reverts erroneous task.
     * <p>
     * Use this function only after canceling related {@literal Future}.
     * <p>
     * If task was of type {@link Task.Type#CREATE_BACKUP} then also related {@link BackupProperties} will be deleted.
     *
     * @param task the instance of {@link Task}.
     */
    public void revertTask(@NotNull Task task) {
        Objects.requireNonNull(task);

        Task.State state = task.getState();
        Optional<BackupProperties> optionalBackupProperties = backupPropertiesManager.findById(task.getBackupPropertiesId());
        if (!optionalBackupProperties.isPresent()) {
            logger.error("Can't revert task: no related backup properties. Task info: {}", task);
            return;
        }

        BackupProperties backupProperties = optionalBackupProperties.get();

        switch (state) {
            case DOWNLOADING:
            case APPLYING_DEPROCESSORS:
            case RESTORING:
            case DELETING: {
                logger.error("Handling broken operation. Operation: {}. No extra actions required", state.toString());

                break;
            }
            case CREATING:
            case APPLYING_PROCESSORS: {
                logger.error("Handling broken operation. Operation: {}. Delete backup properties...", state.toString());

                backupPropertiesManager.deleteById(backupProperties.getId());

                break;
            }
            case UPLOADING: {
                logger.error("Handling broken operation. Operation: {}. Deleting backup from storage...", state);

                Integer deletionTaskId = initNewTask(Task.Type.DELETE_BACKUP, Task.RunType.INTERNAL, backupProperties);
                tasksStarterService.startDeleteTask(deletionTaskId, backupProperties, logger);

                backupPropertiesManager.deleteById(backupProperties.getId());

                break;
            }
            default: {
                logger.error("Can't revert task: unknown state. Task info: {}", task);
            }
        }
    }

    /**
     * Removes task entity.
     *
     * @param id task ID
     */
    public void removeTask(Integer id) {
        tasksRepository.findById(id).ifPresent(backupTask -> tasksRepository.delete(backupTask));
    }

    /**
     * Retrieves an {@literal Task} by its id.
     *
     * @param id entity ID
     * @return the entity with the given id or {@literal Optional#empty()} if none found
     * @throws IllegalArgumentException if {@code id} is {@literal null}.
     */
    public Optional<Task> findById(@NotNull Integer id) {
        return tasksRepository.findById(id);
    }

    /**
     * Returns all {@literal Task} entities of specified {@link Task.RunType}.
     *
     * @param runType run type
     * @return entities as {@literal Iterable<Task>}.
     */
    public Iterable<Task> findAllByRunType(Task.RunType runType) {
        return tasksRepository.findAllByRunType(runType);
    }

    /**
     * Returns all tasks sorted by date in descending order.
     *
     * @return tasks as {@literal Iterable<Task>}.
     */
    public Iterable<Task> findAllByOrderByDateDesc() {
        return tasksRepository.findAllByOrderByDateDesc();
    }
}
