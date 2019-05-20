package com.blog.manager;

import com.blog.controllers.WebControllersConfiguration;
import com.blog.entities.backup.BackupProperties;
import com.blog.entities.backup.BackupTask;
import com.blog.repositories.BackupTaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

/**
 * This manager class provides API to work with {@link WebControllersConfiguration#executorService()} tasks.
 */
@Service
public class BackupTaskManager {
    private static final BackupTask.State initialBackupTaskState = BackupTask.State.PLANNED;
    private static final ConcurrentHashMap<Integer, Future> tasks = new ConcurrentHashMap<>();
    private BackupTaskRepository backupTaskRepository;

    @Autowired
    public void setBackupTaskRepository(BackupTaskRepository backupTaskRepository) {
        this.backupTaskRepository = backupTaskRepository;
    }

    /**
     * Use this method to create new {@link BackupTask}.
     *
     * @param type             backup task type
     * @param backupProperties backup properties of created or being created backup
     * @return ID of created task
     */
    public Integer initNewTask(BackupTask.Type type, BackupProperties backupProperties) {
        BackupTask backupTask = new BackupTask();
        backupTask.setBackupPropertiesId(backupProperties.getId());
        backupTask.setType(type);
        backupTask.setState(initialBackupTaskState);
        backupTask.setError(Boolean.FALSE);

        return backupTaskRepository.save(backupTask).getId();
    }

    /**
     * Adds Future task.
     * <p>
     * This method should be always called when ExecutorService started new task.
     *
     * @param id   ID of task
     * @param task Future task
     */
    public void addTaskFuture(Integer id, Future task) {
        tasks.put(id, task);
    }

    /**
     * Updates task state.
     *
     * @param id    ID of task
     * @param state state to set
     */
    public void updateTaskState(Integer id, BackupTask.State state) {
        BackupTask backupTask = backupTaskRepository.findById(id).orElseThrow(RuntimeException::new);
        backupTask.setState(state);
        backupTaskRepository.save(backupTask);
    }

    /**
     * Returns Future task
     *
     * @param id ID of task
     * @return Optional Future task
     */
    public Optional<Future> getTaskFuture(Integer id) {
        return Optional.ofNullable(tasks.get(id));
    }

    /**
     * Mark currently executing task as erroneous.
     *
     * @param id ID of task
     */
    public void setError(Integer id) {
        BackupTask backupTask = backupTaskRepository.findById(id).orElseThrow(RuntimeException::new);
        backupTask.setError(Boolean.TRUE);
        backupTaskRepository.save(backupTask);
    }

    /**
     * Removes task.
     * <p>
     * This methods removes task from database and also removes Future task.
     *
     * @param id ID of task
     */
    public void removeTask(Integer id) {
        backupTaskRepository.findById(id).ifPresent(backupTask -> backupTaskRepository.delete(backupTask));
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
