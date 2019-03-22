package com.example.demo.StorageManager;

import com.example.demo.models.storage.LocalFileSystemSettings;
import com.example.demo.models.storage.StorageSettings;
import com.example.demo.repositories.storage.StorageSettingsRepository;

public class StorageManager {
    private StorageSettingsRepository storageSettingsRepository;

    public StorageSettings saveStorageSettings(StorageSettings storageSettings) {
        return storageSettingsRepository.save(storageSettings);
    }
}
