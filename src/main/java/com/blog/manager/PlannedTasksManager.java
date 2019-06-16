package com.blog.manager;

import com.blog.entities.task.PlannedTask;
import com.blog.repositories.PlannedTasksRepository;
import com.blog.service.processor.ProcessorType;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * This class provides API to manage planned backup tasks.
 *
 * @see PlannedTask
 */
@Component
@Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRES_NEW)
public class PlannedTasksManager {
    private PlannedTasksRepository plannedTasksRepository;

    @Autowired
    public void setPlannedTasksRepository(PlannedTasksRepository plannedTasksRepository) {
        this.plannedTasksRepository = plannedTasksRepository;
    }

    /**
     * Returns first N rows and sets pessimistic lock on them. Skips already locked rows.
     *
     * @param size  how many rows to retrieve
     * @param state entity state
     * @return first N not locked entities
     */
    public Iterable<PlannedTask> findFirstNByStateAndLock(@NotNull Integer size, @NotNull PlannedTask.State state) {
        return plannedTasksRepository.findFirstNByStateAndLock(size, state);
    }

    /**
     * Creates a new instance of {@link PlannedTask}.
     *
     * @param databaseSettingsName    name of related {@literal DatabaseSettings}
     * @param storageSettingsNameList names of related {@literal StorageSettings}
     * @param processors              processors to apply on backup when starting planned task
     * @param interval                interval between previous start and next start of planned task
     * @return saved entity
     */
    public PlannedTask addNewTask(@NotNull String databaseSettingsName, @NotNull List<String> storageSettingsNameList,
                                  @NotNull List<ProcessorType> processors, @NotNull Long interval) {
        Objects.requireNonNull(databaseSettingsName);
        Objects.requireNonNull(storageSettingsNameList);
        Objects.requireNonNull(processors);
        Objects.requireNonNull(interval);

        PlannedTask plannedTask = new PlannedTask.Builder()
                .withState(PlannedTask.State.WAITING)
                .withDatabaseSettingsName(databaseSettingsName)
                .withStorageSettingsNameList(storageSettingsNameList)
                .withLastStartedTime(LocalDateTime.now(ZoneOffset.UTC))
                .withHandlerTaskId(null)
                .withProcessors(processors)
                .withInterval(Duration.ofSeconds(interval))
                .build();

        return plannedTasksRepository.save(plannedTask);
    }

    /**
     * Updates {@link PlannedTask.State} column of the entity with the given id.
     *
     * @param id    entity ID
     * @param state new state to set
     */
    public void updateState(@NotNull Integer id, @NotNull PlannedTask.State state) {
        plannedTasksRepository.findById(id).ifPresent(
                plannedTask -> plannedTask.setState(state));
    }

    /**
     * Sets last start time to now causing task timer to reset.
     *
     * @param id entity ID
     */
    public void updateLastStartedTimeWithNow(@NotNull Integer id) {
        plannedTasksRepository.findById(id).ifPresent(
                plannedBackupTask -> plannedBackupTask.setLastStartedTime(LocalDateTime.now(ZoneOffset.UTC)));
    }

    /**
     * Attempts to delete the entity with the given id if the one exists.
     *
     * @param id entity ID
     */
    public void deleteById(@NotNull Integer id) {
        plannedTasksRepository.findById(id).ifPresent(
                plannedBackupTask -> plannedTasksRepository.delete(plannedBackupTask));
    }

    /**
     * Retrieves an entity by its id.
     *
     * @param id entity ID
     * @return the entity with the given id or {@literal Optional#empty()} if none found
     */
    public Optional<PlannedTask> findById(Integer id) {
        return plannedTasksRepository.findById(id);
    }
}
