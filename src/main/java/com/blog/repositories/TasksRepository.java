package com.blog.repositories;

import com.blog.entities.task.Task;
import org.springframework.data.repository.CrudRepository;

public interface TasksRepository extends CrudRepository<Task, Integer> {
    Iterable<Task> findAllByOrderByDateDesc();

    Iterable<Task> findAllByRunTypeOrderByDateDesc(Task.RunType runType);
}
