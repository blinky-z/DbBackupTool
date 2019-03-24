package com.example.demo.repositories;

import com.example.demo.entities.database.Database;
import com.example.demo.entities.database.DatabaseSettings;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

public interface DatabaseSettingsRepository extends CrudRepository<DatabaseSettings, Integer> {
    public Iterable<DatabaseSettings> getAllByType(Database type);
}
