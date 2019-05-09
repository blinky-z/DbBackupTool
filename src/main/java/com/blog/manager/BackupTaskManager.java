package com.blog.manager;

import com.blog.entities.backup.BackupProperties;
import com.blog.entities.backup.BackupTask;
import com.blog.entities.backup.BackupTaskState;
import com.blog.entities.backup.BackupTaskType;
import com.blog.repositories.BackupTaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.Future;

@Service
public class BackupTaskManager {
    private static final BackupTaskState initialBackupTaskState = BackupTaskState.PLANNED;
    private static final HashMap<Integer, Future> tasks = new HashMap<>();
    private BackupTaskRepository backupTaskRepository;

    @Autowired
    public void setBackupTaskRepository(BackupTaskRepository backupTaskRepository) {
        this.backupTaskRepository = backupTaskRepository;
    }

    public Integer initNewTask(BackupTaskType type, BackupProperties backupProperties) {
        BackupTask backupTask = new BackupTask();
        backupTask.setBackupPropertiesId(backupProperties.getId());
        backupTask.setType(type);
        backupTask.setState(initialBackupTaskState);
        backupTask.setError(Boolean.FALSE);

        return backupTaskRepository.save(backupTask).getId();
    }

    public void addTaskFuture(Integer id, Future task) {
        tasks.put(id, task);
    }

    public void updateTaskState(Integer id, BackupTaskState state) {
        BackupTask backupTask = backupTaskRepository.findById(id).orElseThrow(RuntimeException::new);
        backupTask.setState(state);
        backupTaskRepository.save(backupTask);
    }

    public Future getTaskFuture(Integer id) {
        return tasks.get(id);
    }

    public void setError(Integer id) {
        BackupTask backupTask = backupTaskRepository.findById(id).orElseThrow(RuntimeException::new);
        backupTask.setError(Boolean.TRUE);
        backupTaskRepository.save(backupTask);
    }

    public void removeTask(Integer id) {
        backupTaskRepository.deleteById(id);
        tasks.remove(id);
    }

    public Optional<BackupTask> getBackupTask(Integer id) {
        return backupTaskRepository.findById(id);
    }

    public Iterable<BackupTask> getBackupTasks() {
        return backupTaskRepository.findAll();
    }

    public Iterable<BackupTask> findAllByOrderByDateDesc() {
        return backupTaskRepository.findAllByOrderByDateDesc();
    }
}
