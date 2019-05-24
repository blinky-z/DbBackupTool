package com.blog.repositories;

import com.blog.entities.backup.BackupTask;
import org.springframework.data.repository.CrudRepository;

public interface BackupTaskRepository extends CrudRepository<BackupTask, Integer> {
    Iterable<BackupTask> findAllByOrderByDateDesc();

    Iterable<BackupTask> findAllByRunType(BackupTask.RunType runType);
}
