package com.blog.repositories;

import com.blog.entities.backup.BackupProperties;
import org.springframework.data.repository.CrudRepository;

import java.util.ArrayList;

public interface BackupPropertiesRepository extends CrudRepository<BackupProperties, Integer> {
    ArrayList<BackupProperties> findAllByOrderByIdDesc();
}
