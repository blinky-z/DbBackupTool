package com.blog.repositories;

import com.blog.entities.backup.PlannedBackupTask;
import org.springframework.data.repository.CrudRepository;

public interface PlannedBackupTasksRepository extends CrudRepository<PlannedBackupTask, Integer> {

}
