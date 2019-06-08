package com.blog.manager;

import com.blog.entities.backup.BackupProperties;
import com.blog.repositories.BackupPropertiesRepository;
import com.blog.service.storage.StorageConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * This class provides API to manage backup properties.
 *
 * @see BackupProperties
 */
@Component
@Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRES_NEW)
public class BackupPropertiesManager {
    private BackupPropertiesRepository backupPropertiesRepository;

    @Autowired
    public void setBackupPropertiesRepository(BackupPropertiesRepository backupPropertiesRepository) {
        this.backupPropertiesRepository = backupPropertiesRepository;
    }

    /**
     * Creates a new instance of {@link BackupProperties}.
     *
     * @param storageSettingsNameList list of storage settings identifiers where backup will be uploaded to
     * @param processors              processors that applies on backup
     * @param databaseName            database name of database of which backup was created
     * @return new BackupProperties with required fields set
     */
    public BackupProperties initNewBackupProperties(@NotNull List<String> storageSettingsNameList, @Nullable List<String> processors,
                                                    @NotNull String databaseName) {
        LocalDateTime creationTime = LocalDateTime.now(ZoneOffset.UTC);
        String backupName = String.format(
                StorageConstants.BACKUP_NAME_TEMPLATE, databaseName, StorageConstants.dateFormatter.format(creationTime));

        if (processors == null) {
            processors = Collections.emptyList();
        }

        return backupPropertiesRepository.save(new BackupProperties(backupName, processors, creationTime, storageSettingsNameList));
    }

    /**
     * Creates a new instance of {@link BackupProperties} with single storage.
     *
     * @param storageSettingsName identifier of storage settings where backup will be uploaded to
     * @param processors          processors that applies on backup
     * @param databaseName        database name of database of which backup was created
     * @return new BackupProperties with required fields set
     */
    public BackupProperties initNewBackupProperties(@NotNull String storageSettingsName, @Nullable List<String> processors,
                                                    @NotNull String databaseName) {
        return initNewBackupProperties(Collections.singletonList(storageSettingsName), processors, databaseName);
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
