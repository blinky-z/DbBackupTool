package com.example.demo.repositories;

import com.example.demo.entities.storage.Storage;
import com.example.demo.entities.storage.StorageSettings;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

public interface StorageSettingsRepository extends CrudRepository<StorageSettings, Integer> {
    public Iterable<StorageSettings> getAllByType(Storage storage);
}
