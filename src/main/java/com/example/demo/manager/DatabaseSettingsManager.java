package com.example.demo.manager;

import com.example.demo.entities.database.Database;
import com.example.demo.entities.database.DatabaseSettings;
import com.example.demo.repositories.DatabaseSettingsRepository;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Component
public class DatabaseSettingsManager {
    private DatabaseSettingsRepository databaseSettingsRepository;

    @Autowired
    public void setDatabaseSettingsRepository(@NotNull DatabaseSettingsRepository databaseSettingsRepository) {
        this.databaseSettingsRepository = databaseSettingsRepository;
    }

    public DatabaseSettings saveDatabaseSettings(@NotNull DatabaseSettings databaseSettings) {
        return databaseSettingsRepository.save(databaseSettings);
    }

    public void deleteDatabaseSettings(@NotNull Integer id) {
        databaseSettingsRepository.deleteById(id);
    }

    public Iterable<DatabaseSettings> getAll() {
        return databaseSettingsRepository.findAll();
    }

    public Optional<DatabaseSettings> getById(@NotNull Integer id) {
        return databaseSettingsRepository.findById(id);
    }

    public Iterable<DatabaseSettings> getAllByType(@NotNull Database type) {
        return databaseSettingsRepository.getAllByType(type);
    }
}
