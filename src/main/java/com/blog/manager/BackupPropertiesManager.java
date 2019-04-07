package com.blog.manager;

import com.blog.entities.backup.BackupProperties;
import com.blog.repositories.BackupPropertiesRepository;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class BackupPropertiesManager {
    private BackupPropertiesRepository backupPropertiesRepository;

    @Autowired
    public void setBackupPropertiesRepository(BackupPropertiesRepository backupPropertiesRepository) {
        this.backupPropertiesRepository = backupPropertiesRepository;
    }

    public BackupProperties save(@NotNull BackupProperties backupProperties) {
        return backupPropertiesRepository.save(backupProperties);
    }

    public Optional<BackupProperties> getById(@NotNull Integer id) {
        return backupPropertiesRepository.findById(id);
    }

    public Iterable<BackupProperties> getAll() {
        return backupPropertiesRepository.findAll();
    }

    public void deleteById(@NotNull Integer id) {
        backupPropertiesRepository.deleteById(id);
    }
}
