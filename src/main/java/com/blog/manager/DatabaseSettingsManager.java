package com.blog.manager;

import com.blog.entities.database.DatabaseSettings;
import com.blog.entities.database.DatabaseType;
import com.blog.repositories.DatabaseSettingsRepository;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * This manager class wraps {@link DatabaseSettingsRepository} and adds extra logic for calls.
 */
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

    public Iterable<DatabaseSettings> saveAll(@NotNull List<DatabaseSettings> databaseSettings) {
        return databaseSettingsRepository.saveAll(databaseSettings);
    }

    public void deleteById(@NotNull String id) {
        databaseSettingsRepository.findById(id).ifPresent(databaseSettings -> databaseSettingsRepository.delete(databaseSettings));
    }

    public Optional<DatabaseSettings> getById(@NotNull String id) {
        return databaseSettingsRepository.findById(id);
    }

    public Iterable<DatabaseSettings> getAllByType(@NotNull DatabaseType type) {
        return databaseSettingsRepository.getAllByType(type);
    }
}
