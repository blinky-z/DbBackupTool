package com.blog.manager;

import com.blog.entities.storage.StorageType;
import com.blog.entities.storage.StorageSettings;
import com.blog.repositories.StorageSettingsRepository;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class StorageSettingsManager {
    private StorageSettingsRepository storageSettingsRepository;

    @Autowired
    public void setStorageSettingsRepository(@NotNull StorageSettingsRepository storageSettingsRepository) {
        this.storageSettingsRepository = storageSettingsRepository;
    }

    public StorageSettings save(@NotNull StorageSettings storageSettings) {
        return storageSettingsRepository.save(storageSettings);
    }

    public boolean existsById(@NotNull String id) {
        return storageSettingsRepository.existsById(id);
    }

    public void deleteById(@NotNull String id) {
        storageSettingsRepository.deleteById(id);
    }

    public Optional<StorageSettings> getById(@NotNull String id) {
        return storageSettingsRepository.findById(id);
    }

    public Iterable<StorageSettings> getAll() {
        return storageSettingsRepository.findAll();
    }

    public Iterable<StorageSettings> getAllByType(@NotNull StorageType type) {
        return storageSettingsRepository.getAllByType(type);
    }
}
