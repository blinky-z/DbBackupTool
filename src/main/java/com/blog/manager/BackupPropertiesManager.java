package com.blog.manager;

import com.blog.entities.backup.BackupProperties;
import com.blog.entities.storage.StorageSettings;
import com.blog.repositories.BackupPropertiesRepository;
import com.blog.service.storage.StorageConstants;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * This manager class wraps {@link BackupPropertiesRepository} and adds extra logic for calls.
 */
@Component
public class BackupPropertiesManager {
    private BackupPropertiesRepository backupPropertiesRepository;

    @Autowired
    public void setBackupPropertiesRepository(BackupPropertiesRepository backupPropertiesRepository) {
        this.backupPropertiesRepository = backupPropertiesRepository;
    }

    /**
     * Use this method to build new {@link BackupProperties} before uploading backup to storage.
     *
     * @param storageSettings storage settings where backup will be uploaded to
     * @param processors      processors that applies on backup
     * @param databaseName    database name of database of which backup was created
     * @return new BackupProperties with required fields set
     */
    public BackupProperties initNewBackupProperties(@NotNull StorageSettings storageSettings, @NotNull List<String> processors,
                                                    @NotNull String databaseName) {
        LocalDateTime creationTime = LocalDateTime.now(ZoneOffset.UTC);
        String backupName = String.format(
                StorageConstants.BACKUP_NAME_TEMPLATE, databaseName, StorageConstants.dateFormatter.format(creationTime));

        BackupProperties backupProperties =
                new BackupProperties(backupName, processors, creationTime, storageSettings.getSettingsName());
        return backupPropertiesRepository.save(backupProperties);
    }

    public Optional<BackupProperties> findById(@NotNull Integer id) {
        return backupPropertiesRepository.findById(id);
    }

    public Iterable<BackupProperties> findAll() {
        return backupPropertiesRepository.findAll();
    }

    public boolean existsById(@NotNull Integer id) {
        return backupPropertiesRepository.existsById(id);
    }

    public ArrayList<BackupProperties> findAllByOrderByIdDesc() {
        return backupPropertiesRepository.findAllByOrderByIdDesc();
    }

    public void deleteById(@NotNull Integer id) {
        backupPropertiesRepository.findById(id).ifPresent(
                backupProperties -> backupPropertiesRepository.delete(backupProperties));
    }
}
