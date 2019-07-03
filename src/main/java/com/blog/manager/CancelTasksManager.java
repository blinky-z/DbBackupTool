package com.blog.manager;

import com.blog.entities.task.CancelTask;
import com.blog.repositories.CancelTasksRepository;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;

@Component
@Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRES_NEW)
public class CancelTasksManager {
    private CancelTasksRepository cancelTasksRepository;

    @Autowired
    public void setCancelTasksRepository(CancelTasksRepository cancelTasksRepository) {
        this.cancelTasksRepository = cancelTasksRepository;
    }

    public Iterable<CancelTask> findAll() {
        return cancelTasksRepository.findAll();
    }

    public void deleteByTaskId(@NotNull Integer taskId) {
        cancelTasksRepository.findByTaskId(taskId).ifPresent(
                cancelTask -> cancelTasksRepository.delete(cancelTask)
        );
    }

    public boolean existsByTaskId(@NotNull Integer taskId) {
        return cancelTasksRepository.existsByTaskId(taskId);
    }

    public void addTaskToCancel(@NotNull Integer taskId) {
        CancelTask cancelTask = new CancelTask();
        cancelTask.setTaskId(taskId);
        cancelTask.setPutTime(LocalDateTime.now(ZoneOffset.UTC));
        cancelTasksRepository.save(cancelTask);
    }

    public void deleteByTaskIdIn(@NotNull Collection<Integer> taskId) {
        cancelTasksRepository.deleteByTaskIdIn(taskId);
    }
}
