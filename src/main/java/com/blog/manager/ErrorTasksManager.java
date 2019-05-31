package com.blog.manager;

import com.blog.entities.backup.ErrorTask;
import com.blog.repositories.ErrorTasksRepository;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
     * Returns page of tasks.
     *
     * @param page {@literal Pageable} instance describing page and limit.
     * @return page of tasks.
     */
    public Page<ErrorTask> findAll(Pageable page) {
        return errorTasksRepository.findAll(page);
    }

    /**
     * Mark currently executing {@link com.blog.entities.backup.Task} as erroneous.
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
     * Checks whether {@link com.blog.entities.backup.Task} have error or not.
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
