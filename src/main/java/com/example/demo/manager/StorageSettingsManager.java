package com.example.demo.manager;

import com.example.demo.entities.storage.Storage;
import com.example.demo.entities.storage.StorageSettings;
import com.example.demo.repositories.StorageSettingsRepository;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Component
public class StorageSettingsManager {
    private StorageSettingsRepository storageSettingsRepository;

    @Autowired
    public void setStorageSettingsRepository(@NotNull StorageSettingsRepository storageSettingsRepository) {
        this.storageSettingsRepository = storageSettingsRepository;
    }

    public StorageSettings saveStorageSettings(@NotNull StorageSettings storageSettings) {
        return storageSettingsRepository.save(storageSettings);
    }

    public void deleteStorageSettings(@NotNull Integer id) {
        storageSettingsRepository.deleteById(id);
    }

    public Optional<StorageSettings> getById(@NotNull Integer id) {
        return storageSettingsRepository.findById(id);
    }

    public Iterable<StorageSettings> getAll() {
        return storageSettingsRepository.findAll();
    }

    public Iterable<StorageSettings> getAllByType(@NotNull Storage type) {
        return storageSettingsRepository.getAllByType(type);
    }
}
