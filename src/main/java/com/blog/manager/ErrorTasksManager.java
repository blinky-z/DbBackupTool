package com.blog.manager;

import com.blog.entities.task.ErrorTask;
import com.blog.entities.task.Task;
import com.blog.repositories.ErrorTasksRepository;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * This class provides API to manage error tasks.
 */
@Component
@Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRES_NEW)
public class ErrorTasksManager {
    private ErrorTasksRepository errorTasksRepository;

    @Autowired
    public void setErrorTasksRepository(ErrorTasksRepository errorTasksRepository) {
        this.errorTasksRepository = errorTasksRepository;
    }

    /**
     * Returns first N rows and sets pessimistic lock on them. Skips already locked rows.
     *
     * @param size how many rows to retrieve
     * @return first N entities
     */
    public Iterable<ErrorTask> findFirstN(@NotNull Integer size) {
        return errorTasksRepository.findFirstN(size);
    }

    /**
     * Mark currently executing {@link Task} as erroneous.
     *
     * @param taskId task ID
     */
    public void setError(@NotNull Integer taskId) {
        if (!errorTasksRepository.existsByTaskId(taskId)) {
            ErrorTask entity = new ErrorTask();
            entity.setTaskId(taskId);
            errorTasksRepository.save(entity);
        }
    }

    /**
     * Checks whether {@link Task} have error or not.
     *
     * @param taskId task ID
     * @return {@literal true} if task have error and {@literal false} otherwise
     */
    public boolean isError(@NotNull Integer taskId) {
        return errorTasksRepository.existsByTaskId(taskId);
    }

    public void setErrorHandled(@NotNull Integer taskId) {
        errorTasksRepository.findByTaskId(taskId).ifPresent(
                backupTask -> backupTask.setErrorHandled(Boolean.TRUE));
    }
}
