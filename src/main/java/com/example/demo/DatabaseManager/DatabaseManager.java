package com.example.demo.DatabaseManager;

import com.example.demo.entities.database.DatabaseSettings;
import com.example.demo.repositories.database.DatabaseSettingsRepository;
import com.example.demo.repositories.storage.StorageSettingsRepository;

public class DatabaseManager {
    private static DatabaseSettingsRepository databaseSettingsRepository;

    public DatabaseSettings saveDatabaseSettings(DatabaseSettings databaseSettings) {
        return databaseSettingsRepository.save(databaseSettings);
    }

}
