package com.example.demo.manager;

import com.example.demo.entities.database.DatabaseSettings;
import com.example.demo.entities.database.DatabaseType;
import com.example.demo.repositories.DatabaseSettingsRepository;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class DatabaseSettingsManager {
    private DatabaseSettingsRepository databaseSettingsRepository;

    @Autowired
    public void setDatabaseSettingsRepository(@NotNull DatabaseSettingsRepository databaseSettingsRepository) {
        this.databaseSettingsRepository = databaseSettingsRepository;
    }

    public DatabaseSettings save(@NotNull DatabaseSettings databaseSettings) {
        return databaseSettingsRepository.save(databaseSettings);
    }

    public boolean existsById(@NotNull String id) {
        return databaseSettingsRepository.existsById(id);
    }

    public void deleteById(@NotNull String id) {
        databaseSettingsRepository.deleteById(id);
    }

    public Iterable<DatabaseSettings> getAll() {
        return databaseSettingsRepository.findAll();
    }

    public Optional<DatabaseSettings> getById(@NotNull String id) {
        return databaseSettingsRepository.findById(id);
    }

    public Iterable<DatabaseSettings> getAllByType(@NotNull DatabaseType type) {
        return databaseSettingsRepository.getAllByType(type);
    }
}
