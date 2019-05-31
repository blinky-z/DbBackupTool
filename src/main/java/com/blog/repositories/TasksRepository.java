package com.blog.repositories;

import com.blog.entities.backup.Task;
import org.springframework.data.repository.CrudRepository;

public interface TasksRepository extends CrudRepository<Task, Integer> {
    Iterable<Task> findAllByOrderByDateDesc();

    Iterable<Task> findAllByRunType(Task.RunType runType);
}
