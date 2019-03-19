package com.example.demo.repositories.storage;

import com.example.demo.models.storage.DropboxSettings;
import org.springframework.data.repository.CrudRepository;

public interface DropboxSettingsStorageRepository extends CrudRepository<DropboxSettings, Integer> {

}
