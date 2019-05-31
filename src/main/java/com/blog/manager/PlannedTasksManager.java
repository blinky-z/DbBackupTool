package com.blog.manager;

import com.blog.entities.backup.PlannedTask;
import com.blog.repositories.PlannedTasksRepository;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * This class provides API to manage planned backup tasks.
 */
@Component
@Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRES_NEW)
public class PlannedTasksManager {
    private PlannedTasksRepository plannedTasksRepository;

    @Autowired
    public void setPlannedTasksRepository(PlannedTasksRepository plannedTasksRepository) {
        this.plannedTasksRepository = plannedTasksRepository;
    }

    public Optional<PlannedTask> findById(Integer id) {
        return plannedTasksRepository.findById(id);
    }

    public Iterable<PlannedTask> findAllByState(Pageable page, PlannedTask.State state) {
        return plannedTasksRepository.findAllByState(page, state);
    }

    /**
     * Use this method to add a new planned task.
     *
     * @param databaseSettingsName    name of related {@literal DatabaseSettings}
     * @param storageSettingsNameList names of related {@literal StorageSettings}
     * @param processors              processors to apply on backup when starting planned task
     * @param interval                interval between previous start and next start of planned task
     * @return created {@literal PlannedTask} entity.
     */
    public PlannedTask addNewTask(@NotNull String databaseSettingsName, @NotNull List<String> storageSettingsNameList,
                                  @NotNull List<String> processors, @NotNull Long interval) {
        Objects.requireNonNull(databaseSettingsName);
        Objects.requireNonNull(storageSettingsNameList);
        Objects.requireNonNull(processors);
        Objects.requireNonNull(interval);

        PlannedTask plannedTask = new PlannedTask.Builder()
                .withState(PlannedTask.State.WAITING)
                .withDatabaseSettingsName(databaseSettingsName)
                .withStorageSettingsNameList(storageSettingsNameList)
                .withLastStartedTime(LocalDateTime.now(ZoneOffset.UTC))
                .withExecutingTasks(Collections.emptyList())
                .withProcessors(processors)
                .withInterval(Duration.ofSeconds(interval))
                .build();

        return plannedTasksRepository.save(plannedTask);
    }

    /**
     * Sets in entity IDs of tasks that handles this planned this.
     * <p>
     * Call this method when planned task started (i.e. planned task fired and tasks was started).
     *
     * @param plannedTaskId  planned task ID
     * @param executingTasks {@literal Task} IDs.
     */
    public void setExecutingTasks(@NotNull Integer plannedTaskId, @NotNull List<Integer> executingTasks) {
        plannedTasksRepository.findById(plannedTaskId).ifPresent(
                plannedBackupTask -> plannedBackupTask.setExecutingTasks(executingTasks));
    }

    /**
     * Removes from entity IDs of tasks that were handling this planned task.
     * <p>
     * Call this method when all related tasks completed.
     *
     * @param plannedTaskId planned task ID
     */
    public void clearExecutingTasks(@NotNull Integer plannedTaskId) {
        plannedTasksRepository.findById(plannedTaskId).ifPresent(
                plannedBackupTask -> plannedBackupTask.setExecutingTasks(Collections.emptyList()));
    }

    /**
     * Updates planned task state.
     *
     * @param plannedTaskId planned task ID
     * @param state         new state to set
     */
    public void updateState(@NotNull Integer plannedTaskId, @NotNull PlannedTask.State state) {
        plannedTasksRepository.findById(plannedTaskId).ifPresent(
                plannedTask -> plannedTask.setState(state));
    }

    /**
     * Deletes planned task.
     * <p>
     * This method doesn't throw exception of no such entity exists.
     *
     * @param id planned task ID
     */
    public void deleteById(@NotNull Integer id) {
        plannedTasksRepository.findById(id).ifPresent(
                plannedBackupTask -> plannedTasksRepository.delete(plannedBackupTask));
    }

    /**
     * Sets last start time with now causing task timer to reset.
     *
     * @param id planned task ID
     */
    public void updateLastStartedTimeWithNow(@NotNull Integer id) {
        plannedTasksRepository.findById(id).ifPresent(
                plannedBackupTask -> plannedBackupTask.setLastStartedTime(LocalDateTime.now(ZoneOffset.UTC)));
    }
}
