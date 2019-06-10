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
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * This class provides API to manage backup related tasks.
 *
 * @see Task
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
     * @param taskType           backup task type
     * @param runType            task initiator
     * @param backupPropertiesId identifier of backup properties of created or being created backup
     * @return ID of created task
     * @see BackupPropertiesManager#initNewBackupProperties(List, List, String)
     */
    public Integer initNewTask(Task.Type taskType, Task.RunType runType, Integer backupPropertiesId) {
        Task task = new Task.Builder()
                .withType(taskType)
                .withRunType(runType)
                .withBackupPropertiesId(backupPropertiesId)
                .withState(initialBackupTaskState)
                .withDate(LocalDateTime.now(ZoneOffset.UTC))
                .build();

        task = tasksRepository.save(task);

        return task.getId();
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
                logger.info("Handling broken operation. Operation: {}. No extra actions required", state.toString());

                break;
            }
            case CREATING:
            case APPLYING_PROCESSORS: {
                logger.info("Handling broken operation. Operation: {}. Delete backup properties...", state.toString());

                Optional<BackupProperties> optionalBackupProperties = backupPropertiesManager.findById(task.getBackupPropertiesId());
                if (!optionalBackupProperties.isPresent()) {
                    logger.error("Can't revert task: no related backup properties. Task info: {}", task);
                    return;
                }

                BackupProperties backupProperties = optionalBackupProperties.get();

                backupPropertiesManager.deleteById(backupProperties.getId());

                break;
            }
            case UPLOADING: {
                logger.info("Handling broken operation. Operation: {}. Deleting backup from storage...", state);

                Optional<BackupProperties> optionalBackupProperties = backupPropertiesManager.findById(task.getBackupPropertiesId());
                if (!optionalBackupProperties.isPresent()) {
                    logger.error("Can't revert task: no related backup properties. Task info: {}", task);
                    return;
                }

                BackupProperties backupProperties = optionalBackupProperties.get();

                Integer deletionTaskId = initNewTask(Task.Type.DELETE_BACKUP, Task.RunType.INTERNAL, backupProperties.getId());
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
     * Attempts to delete the entity with the given id if the one exists.
     *
     * @param id entity ID
     */
    public void deleteById(Integer id) {
        tasksRepository.findById(id).ifPresent(backupTask -> tasksRepository.delete(backupTask));
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

    /**
     * Returns all instances of the given {@link Task.RunType}.
     *
     * @param runType run type
     * @return all entities of the given {@link Task.RunType}
     */
    public Iterable<Task> findAllByRunType(Task.RunType runType) {
        return tasksRepository.findAllByRunType(runType);
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
