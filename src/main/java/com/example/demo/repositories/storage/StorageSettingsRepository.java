package com.example.demo.repositories.storage;

import com.example.demo.entities.storage.Storage;
import com.example.demo.entities.storage.StorageSettings;
import org.springframework.data.repository.CrudRepository;

public interface StorageSettingsRepository extends CrudRepository<StorageSettings, Integer> {
    public Iterable<StorageSettings> getAllByType(Storage storage);
}