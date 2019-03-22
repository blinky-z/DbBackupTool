package com.example.demo.StorageManager;

import com.example.demo.entities.storage.StorageSettings;
import com.example.demo.repositories.storage.StorageSettingsRepository;

public class StorageManager {
    private static StorageSettingsRepository storageSettingsRepository;

    public StorageSettings saveStorageSettings(StorageSettings storageSettings) {
        return storageSettingsRepository.save(storageSettings);
    }
}
