package com.example.demo.repositories;

import com.example.demo.entities.database.DatabaseSettings;
import com.example.demo.entities.database.DatabaseType;
import org.springframework.data.repository.CrudRepository;

public interface DatabaseSettingsRepository extends CrudRepository<DatabaseSettings, String> {
    Iterable<DatabaseSettings> getAllByType(DatabaseType type);
}
