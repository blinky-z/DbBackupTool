package com.example.demo.DatabaseManager;

import com.example.demo.entities.database.Database;
import com.example.demo.entities.database.DatabaseSettings;
import com.example.demo.repositories.database.DatabaseSettingsRepository;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DatabaseManager {
    private static DatabaseSettingsRepository databaseSettingsRepository;

    @Autowired
    public static void setDatabaseSettingsRepository(DatabaseSettingsRepository databaseSettingsRepository) {
        DatabaseManager.databaseSettingsRepository = databaseSettingsRepository;
    }

    public DatabaseSettings saveDatabaseSettings(DatabaseSettings databaseSettings) {
        return databaseSettingsRepository.save(databaseSettings);
    }

    public void deleteDatabaseSettings(@NotNull Integer id) {
        databaseSettingsRepository.deleteById(id);
    }

    public Iterable<DatabaseSettings> getAll() {
        return databaseSettingsRepository.findAll();
    }

    public Iterable<DatabaseSettings> getAllByType(Database type) {
        return databaseSettingsRepository.getAllByType(type);
    }
}
