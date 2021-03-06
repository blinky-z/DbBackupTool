package com.blog.manager;

import com.blog.entities.backup.BackupProperties;
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
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * This class provides API to manage backup related tasks.
 *
 * @see Task
 */
@Component
@Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
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
     * @param taskType           backup task type
     * @param runType            task initiator
     * @param backupPropertiesId identifier of backup properties of created or being created backup
     * @return ID of created task
     * @see BackupPropertiesManager#initNewBackupProperties(List, List, String)
     */
    public Task initNewTask(Task.Type taskType, Task.RunType runType, Integer backupPropertiesId) {
        Task task = new Task.Builder()
                .withType(taskType)
                .withRunType(runType)
                .withBackupPropertiesId(backupPropertiesId)
                .withState(initialBackupTaskState)
                .withDate(LocalDateTime.now(ZoneOffset.UTC))
                .build();

        return tasksRepository.save(task);
    }

    /**
     * Updates {@link Task.State} column of the entity with the given id.
     *
     * @param id    entity ID
     * @param state new state to set
     */
    public void updateTaskState(@NotNull Integer id, @NotNull Task.State state) {
        tasksRepository.findById(id).ifPresent(task -> task.setState(state));
    }

    /**
     * Set interrupt flag to entity.
     *
     * @param id entity ID
     */
    public void setInterrupted(@NotNull Integer id) {
        tasksRepository.findById(id).ifPresent(task -> task.setInterrupted(Boolean.TRUE));
    }

    /**
     * This function reverts erroneous task by its entity.
     * <p>
     * Use this function only after canceling related {@literal Future}.
     * <p>
     * If the task was of the type {@link Task.Type#CREATE_BACKUP} then related {@link BackupProperties} will be deleted.
     *
     * @param task the entity
     */
    public void revertTask(@NotNull Task task) {
        Objects.requireNonNull(task);

        Task.State state = task.getState();

        switch (state) {
            case DOWNLOADING:
            case APPLYING_DEPROCESSORS:
            case RESTORING:
            case DELETING: {
                logger.info("Handling broken operation. Operation: {}: No extra actions required. Task info: {}", state, task);
                break;
            }
            case CREATING:
            case APPLYING_PROCESSORS: {
                logger.info("Handling broken operation. Operation: {}: Deleting backup properties... Task info: {}", state, task);

                Integer backupPropertiesID = task.getBackupPropertiesId();

                if (!backupPropertiesManager.existsById(backupPropertiesID)) {
                    logger.error("Can't revert task: no related backup properties. Task info: {}", task);
                    return;
                }

                backupPropertiesManager.deleteById(backupPropertiesID);
                break;
            }
            case UPLOADING: {
                logger.info("Handling broken operation. Operation: {}: Deleting backup from storage... Task info: {}", state, task);

                Integer backupPropertiesId = task.getBackupPropertiesId();
                Optional<BackupProperties> optionalBackupProperties = backupPropertiesManager.findById(backupPropertiesId);
                if (optionalBackupProperties.isEmpty()) {
                    logger.error("Can't revert task: no related backup properties. Task info: {}", task);
                    return;
                }

                tasksStarterService.startDeleteTask(Task.RunType.INTERNAL, optionalBackupProperties.get());
                backupPropertiesManager.deleteById(backupPropertiesId);
                break;
            }
            default: {
                logger.error("Can't revert task: invalid state. Task info: {}", task);
            }
        }
    }

    /**
     * Retrieves an entity by its id.
     *
     * @param id entity ID
     * @return the entity with the given id or {@literal Optional#empty()} if none found
     */
    public Optional<Task> findById(@NotNull Integer id) {
        return tasksRepository.findById(id);
    }

    public Iterable<Task> findAllById(@NotNull Collection<Integer> ids) {
        return tasksRepository.findAllById(ids);
    }

    /**
     * Returns all instances of the given {@link Task.RunType}.
     *
     * @param runType run type
     * @return all entities of the given {@link Task.RunType}
     */
    public Iterable<Task> findAllByRunTypeOrderByDateDesc(Task.RunType runType) {
        return tasksRepository.findAllByRunTypeOrderByDateDesc(runType);
    }

    /**
     * Returns all tasks sorted by date in descending order.
     *
     * @return all entities
     */
    public Iterable<Task> findAllByOrderByDateDesc() {
        return tasksRepository.findAllByOrderByDateDesc();
    }
}
