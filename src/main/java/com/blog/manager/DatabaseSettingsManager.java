package com.blog.manager;

import com.blog.entities.database.DatabaseSettings;
import com.blog.entities.database.DatabaseType;
import com.blog.repositories.DatabaseSettingsRepository;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * This class provides API to manage database settings.
 *
 * @see DatabaseSettings
 */
@Component
@Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRES_NEW)
public class DatabaseSettingsManager {
    private DatabaseSettingsRepository databaseSettingsRepository;

    @Autowired
    public void setDatabaseSettingsRepository(@NotNull DatabaseSettingsRepository databaseSettingsRepository) {
        this.databaseSettingsRepository = databaseSettingsRepository;
    }

    /**
     * Saves a given entity.
     *
     * @param entity entity
     * @return the saved entity
     */
    public DatabaseSettings save(@NotNull DatabaseSettings entity) {
        return databaseSettingsRepository.save(entity);
    }

    /**
     * Saves all given entities.
     *
     * @param entities entities to save
     * @return the saved entities
     */
    public Iterable<DatabaseSettings> saveAll(@NotNull List<DatabaseSettings> entities) {
        return databaseSettingsRepository.saveAll(entities);
    }

    /**
     * Attempts to delete the entity with the given id if the one exists.
     *
     * @param id entity ID
     */
    public void deleteById(@NotNull String id) {
        databaseSettingsRepository.findById(id).ifPresent(databaseSettings -> databaseSettingsRepository.delete(databaseSettings));
    }

    /**
     * Returns whether an entity with the given id exists.
     *
     * @param id entity ID
     * @return {@literal true} if an entity with the given id exists, {@literal false} otherwise.
     */
    public boolean existsById(@NotNull String id) {
        return databaseSettingsRepository.existsById(id);
    }

    /**
     * Retrieves an entity by its id.
     *
     * @param id entity ID
     * @return the entity with the given id or {@literal Optional#empty()} if none found
     */
    public Optional<DatabaseSettings> findById(@NotNull String id) {
        return databaseSettingsRepository.findById(id);
    }

    /**
     * Returns all instances of the type of the given {@link DatabaseType}.
     *
     * @param type database type
     * @return all entities of the given {@link DatabaseType}
     */
    public Iterable<DatabaseSettings> findAllByType(@NotNull DatabaseType type) {
        return databaseSettingsRepository.getAllByType(type);
    }
}
