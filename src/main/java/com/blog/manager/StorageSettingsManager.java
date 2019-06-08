package com.blog.manager;

import com.blog.entities.storage.StorageSettings;
import com.blog.entities.storage.StorageType;
import com.blog.repositories.StorageSettingsRepository;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * This class provides API to manage storage settings.
 *
 * @see StorageSettings
 */
@Component
@Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRES_NEW)
public class StorageSettingsManager {
    private StorageSettingsRepository storageSettingsRepository;

    @Autowired
    public void setStorageSettingsRepository(@NotNull StorageSettingsRepository storageSettingsRepository) {
        this.storageSettingsRepository = storageSettingsRepository;
    }

    public StorageSettings save(@NotNull StorageSettings storageSettings) {
        return storageSettingsRepository.save(storageSettings);
    }

    public Iterable<StorageSettings> saveAll(@NotNull List<StorageSettings> storageSettings) {
        return storageSettingsRepository.saveAll(storageSettings);
    }

    public void deleteById(@NotNull String id) {
        storageSettingsRepository.findById(id).ifPresent(storageSettings -> storageSettingsRepository.delete(storageSettings));
    }

    public boolean existsById(@NotNull String id) {
        return storageSettingsRepository.existsById(id);
    }

    public Optional<StorageSettings> findById(@NotNull String id) {
        return storageSettingsRepository.findById(id);
    }

    public Iterable<StorageSettings> findAllByType(@NotNull StorageType type) {
        return storageSettingsRepository.getAllByType(type);
    }
}
