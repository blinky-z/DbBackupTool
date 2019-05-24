package com.blog.manager;

import com.blog.entities.backup.PlannedBackupTask;
import com.blog.repositories.PlannedBackupTasksRepository;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * This class provides API to manage planned backup tasks.
 */
@Component
public class PlannedBackupTasksManager {
    private PlannedBackupTasksRepository plannedBackupTasksRepository;

    @Autowired
    public void setPlannedBackupTasksRepository(PlannedBackupTasksRepository plannedBackupTasksRepository) {
        this.plannedBackupTasksRepository = plannedBackupTasksRepository;
    }

    public Optional<PlannedBackupTask> findById(Integer id) {
        return plannedBackupTasksRepository.findById(id);
    }

    public Iterable<PlannedBackupTask> findAll() {
        return plannedBackupTasksRepository.findAll();
    }

    public PlannedBackupTask addNewTask(PlannedBackupTask.Type type, String databaseSettingsName,
                                        List<String> storageSettingsNameList, List<String> processors, Long interval) {
        PlannedBackupTask plannedBackupTask = new PlannedBackupTask();
        plannedBackupTask.setType(type);
        plannedBackupTask.setDatabaseSettingsName(databaseSettingsName);
        plannedBackupTask.setStorageSettingsNameList(storageSettingsNameList);
        plannedBackupTask.setProcessors(processors);
        plannedBackupTask.setLastStartedTime(Instant.now());
        plannedBackupTask.setInterval(Duration.ofSeconds(interval));

        return plannedBackupTasksRepository.save(plannedBackupTask);
    }

    public void deleteById(@NotNull Integer id) {
        plannedBackupTasksRepository.findById(id).ifPresent(
                plannedBackupTask -> plannedBackupTasksRepository.delete(plannedBackupTask));
    }

    public void updateLastStartedTimeWithCurrent(@NotNull Integer id) {
        plannedBackupTasksRepository.findById(id).ifPresent(plannedBackupTask -> {
            plannedBackupTask.setLastStartedTime(Instant.now());
            plannedBackupTasksRepository.save(plannedBackupTask);
        });
    }
}
