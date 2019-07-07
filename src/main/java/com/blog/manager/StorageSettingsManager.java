package com.blog.manager;

import com.blog.entities.storage.StorageSettings;
import com.blog.entities.storage.StorageType;
import com.blog.repositories.StorageSettingsRepository;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * This class provides API to manage storage settings.
 *
 * @see StorageSettings
 */
@Component
@Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
public class StorageSettingsManager {
    private StorageSettingsRepository storageSettingsRepository;

    @Autowired
    public void setStorageSettingsRepository(@NotNull StorageSettingsRepository storageSettingsRepository) {
        this.storageSettingsRepository = storageSettingsRepository;
    }

    /**
     * Saves a given entity.
     *
     * @param entity entity
     * @return the saved entity
     */
    public StorageSettings save(@NotNull StorageSettings entity) {
        return storageSettingsRepository.save(entity);
    }

    /**
     * Saves all given entities.
     *
     * @param entities entities to save
     * @return the saved entities
     */
    public Iterable<StorageSettings> saveAll(@NotNull List<StorageSettings> entities) {
        return storageSettingsRepository.saveAll(entities);
    }

    /**
     * Attempts to delete the entity with the given id if the one exists.
     *
     * @param id entity ID
     */
    public void deleteById(@NotNull String id) {
        storageSettingsRepository.findById(id).ifPresent(storageSettings -> storageSettingsRepository.delete(storageSettings));
    }

    /**
     * Returns whether an entity with the given id exists.
     *
     * @param id entity ID
     * @return {@literal true} if an entity with the given id exists, {@literal false} otherwise.
     */
    public boolean existsById(@NotNull String id) {
        return storageSettingsRepository.existsById(id);
    }

    /**
     * Retrieves an entity by its id.
     *
     * @param id entity ID
     * @return the entity with the given id or {@literal Optional#empty()} if none found
     */
    public Optional<StorageSettings> findById(@NotNull String id) {
        return storageSettingsRepository.findById(id);
    }

    /**
     * Returns all instances of the type of the given {@link StorageType}.
     *
     * @param type database type
     * @return all entities of the given {@link StorageType}
     */
    public Iterable<StorageSettings> findAllByType(@NotNull StorageType type) {
        return storageSettingsRepository.getAllByType(type);
    }
}
