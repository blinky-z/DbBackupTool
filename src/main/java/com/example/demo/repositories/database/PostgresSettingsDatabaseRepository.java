package com.example.demo.repositories.database;

import com.example.demo.models.database.PostgresSettings;
import org.springframework.data.repository.CrudRepository;

public interface PostgresSettingsDatabaseRepository extends CrudRepository<PostgresSettings, Integer> {

}
