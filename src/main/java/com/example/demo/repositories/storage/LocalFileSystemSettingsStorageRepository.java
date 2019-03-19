package com.example.demo.repositories.storage;

import com.example.demo.models.storage.LocalFileSystemSettings;
import org.springframework.data.repository.CrudRepository;

public interface LocalFileSystemSettingsStorageRepository extends CrudRepository<LocalFileSystemSettings, Integer> {

}
