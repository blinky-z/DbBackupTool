package com.blog.repositories;

import com.blog.entities.task.CancelTask;
import org.springframework.data.repository.CrudRepository;

import java.util.Collection;
import java.util.Optional;

public interface CancelTasksRepository extends CrudRepository<CancelTask, Integer> {
    Optional<CancelTask> findByTaskId(Integer taskId);

    void deleteByTaskIdIn(Collection<Integer> taskId);

    boolean existsByTaskId(Integer taskId);
}
