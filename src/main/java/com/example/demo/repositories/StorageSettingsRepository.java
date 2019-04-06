package com.example.demo.repositories;

import com.example.demo.entities.storage.StorageSettings;
import com.example.demo.entities.storage.StorageType;
import org.springframework.data.repository.CrudRepository;

import java.util.Collection;

public interface StorageSettingsRepository extends CrudRepository<StorageSettings, String> {
    Collection<StorageSettings> getAllByType(StorageType storageType);
}
