package com.blog.repositories;

import com.blog.entities.database.DatabaseSettings;
import com.blog.entities.database.DatabaseType;
import org.springframework.data.repository.CrudRepository;

public interface DatabaseSettingsRepository extends CrudRepository<DatabaseSettings, String> {
    Iterable<DatabaseSettings> getAllByType(DatabaseType type);
}
