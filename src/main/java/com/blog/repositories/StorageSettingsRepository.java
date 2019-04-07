package com.blog.repositories;

import com.blog.entities.storage.StorageType;
import com.blog.entities.storage.StorageSettings;
import org.springframework.data.repository.CrudRepository;

import java.util.Collection;

public interface StorageSettingsRepository extends CrudRepository<StorageSettings, String> {
    Collection<StorageSettings> getAllByType(StorageType storageType);
}
