package com.blog.manager;

import com.blog.entities.backup.BackupProperties;
import com.blog.repositories.BackupPropertiesRepository;
import com.blog.service.processor.ProcessorType;
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
@Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
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
     * @return saved entity
     */
    public BackupProperties initNewBackupProperties(@NotNull List<String> storageSettingsNameList, @Nullable List<ProcessorType> processors,
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
     * @return saved entity
     */
    public BackupProperties initNewBackupProperties(@NotNull String storageSettingsName, @Nullable List<ProcessorType> processors,
                                                    @NotNull String databaseName) {
        return initNewBackupProperties(Collections.singletonList(storageSettingsName), processors, databaseName);
    }

    /**
     * Retrieves an entity by its id.
     *
     * @param id entity ID
     * @return the entity with the given id or {@literal Optional#empty()} if none found
     */
    public Optional<BackupProperties> findById(@NotNull Integer id) {
        return backupPropertiesRepository.findById(id);
    }

    /**
     * Returns all instances of the type.
     *
     * @return all entities
     */
    public Iterable<BackupProperties> findAll() {
        return backupPropertiesRepository.findAll();
    }

    /**
     * Returns whether an entity with the given id exists.
     *
     * @param id entity ID
     * @return {@literal true} if an entity with the given id exists, {@literal false} otherwise.
     */
    public boolean existsById(@NotNull Integer id) {
        return backupPropertiesRepository.existsById(id);
    }

    /**
     * Returns all instances sorted by id in descending order.
     *
     * @return all entities
     */
    public ArrayList<BackupProperties> findAllByOrderByIdDesc() {
        return backupPropertiesRepository.findAllByOrderByIdDesc();
    }

    /**
     * Attempts to delete the entity with the given id if the one exists.
     *
     * @param id entity ID
     */
    public void deleteById(@NotNull Integer id) {
        backupPropertiesRepository.findById(id).ifPresent(
                backupProperties -> backupPropertiesRepository.delete(backupProperties));
    }
}
