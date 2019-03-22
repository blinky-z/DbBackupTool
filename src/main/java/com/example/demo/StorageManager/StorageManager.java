package com.example.demo.StorageManager;

import com.example.demo.entities.storage.Storage;
import com.example.demo.entities.storage.StorageSettings;
import com.example.demo.repositories.storage.StorageSettingsRepository;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StorageManager {
    private static StorageSettingsRepository storageSettingsRepository;

    @Autowired
    public static void setStorageSettingsRepository(StorageSettingsRepository storageSettingsRepository) {
        StorageManager.storageSettingsRepository = storageSettingsRepository;
    }

    public StorageSettings saveStorageSettings(StorageSettings storageSettings) {
        return storageSettingsRepository.save(storageSettings);
    }

    public void deleteStoragesettings(@NotNull Integer id) {
        storageSettingsRepository.deleteById(id);
    }

    public Iterable<StorageSettings> getAll() {
        return storageSettingsRepository.findAll();
    }

    public Iterable<StorageSettings> getAllByType(Storage type) {
        return storageSettingsRepository.getAllByType(type);
    }
}
