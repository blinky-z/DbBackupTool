package com.example.demo.repositories;

import com.example.demo.entities.database.Database;
import com.example.demo.entities.database.DatabaseSettings;
import org.springframework.data.repository.CrudRepository;

public interface DatabaseSettingsRepository extends CrudRepository<DatabaseSettings, Integer> {
    Iterable<DatabaseSettings> getAllByType(Database type);
}
