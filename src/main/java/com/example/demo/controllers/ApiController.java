package com.example.demo.controllers;

import com.example.demo.DbBackup;
import com.example.demo.repositories.database.PostgresSettingsDatabaseRepository;
import com.example.demo.repositories.storage.DropboxSettingsStorageRepository;
import com.example.demo.repositories.storage.LocalFileSystemSettingsStorageRepository;
import com.example.demo.webUi.WebUiSettings.CreateBackupSettings;
import com.example.demo.webUi.WebUiSettings.CreateDatabaseSettings;
import com.example.demo.webUi.WebUiSettings.CreateStorageSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.validation.Valid;

@Controller
public class ApiController {
    private static final Logger logger = LoggerFactory.getLogger(DbBackup.class);

    private LocalFileSystemSettingsStorageRepository localFileSystemSettingsStorageRepository;

    private DropboxSettingsStorageRepository dropboxSettingsStorageRepository;

    private PostgresSettingsDatabaseRepository postgresSettingsDatabaseRepository;

    @Autowired
    public void setLocalFileSystemSettingsStorageRepository(LocalFileSystemSettingsStorageRepository localFileSystemSettingsStorageRepository) {
        this.localFileSystemSettingsStorageRepository = localFileSystemSettingsStorageRepository;
    }

    @Autowired
    public void setDropboxSettingsStorageRepository(DropboxSettingsStorageRepository dropboxSettingsStorageRepository) {
        this.dropboxSettingsStorageRepository = dropboxSettingsStorageRepository;
    }

    @Autowired
    public void setPostgresSettingsDatabaseRepository(PostgresSettingsDatabaseRepository postgresSettingsDatabaseRepository) {
        this.postgresSettingsDatabaseRepository = postgresSettingsDatabaseRepository;
    }

//    TODO: сделать проверку типа стореджа или типа базы данных на основании enum (констант), а не строки.
//     Заменить в thymeleaf все использования типов на такие же константы, чтобы в хандлере я мог принимать сразу константу.

//    TODO: добавить нормальную валидацию всех форм.

    @DeleteMapping(value = "/database")
    public String deleteDatabase(@RequestParam(value = "databaseType", required = true) String databaseType,
                                 @RequestParam(value = "id", required = true) int id) {
        switch (databaseType) {
            case "PostgreSQL": {
                postgresSettingsDatabaseRepository.deleteById(id);
                break;
            }
        }

        return "redirect:/dashboard";
    }

    @PostMapping(value = "/database")
    public String createDatabase(@Valid CreateDatabaseSettings createDatabaseSettings, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            logger.info("Has errors");
            logger.info(bindingResult.getAllErrors().toString());
        }

        String databaseType = createDatabaseSettings.getDatabaseType();
        switch (databaseType) {
            case "postgres": {
                postgresSettingsDatabaseRepository.save(createDatabaseSettings.getPostgresSettings());
                break;
            }
        }

        return "redirect:/dashboard";
    }

    @DeleteMapping(value = "/storage")
    public String deleteStorage(@RequestParam(value = "storageType", required = true) String storageType,
                                @RequestParam(value = "id", required = true) int id) {
        logger.info("Deletion of storage: storage type: {}, id: {}", storageType, id);

        switch (storageType) {
            case "Dropbox": {
                dropboxSettingsStorageRepository.deleteById(id);
                break;
            }
            case "Local File System": {
                localFileSystemSettingsStorageRepository.deleteById(id);
                break;
            }
        }

        return "redirect:/dashboard";
    }

    @PostMapping(value = "/storage")
    public String createStorage(@Valid CreateStorageSettings createStorageSettings, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            logger.info("Has errors");
            logger.info(bindingResult.getAllErrors().toString());
        }

        String storageType = createStorageSettings.getStorageType();
        switch (storageType) {
            case "dropbox": {
                dropboxSettingsStorageRepository.save(createStorageSettings.getDropboxSettings());
                break;
            }
            case "localFileSystem": {
                localFileSystemSettingsStorageRepository.save(createStorageSettings.getLocalFileSystemSettings());
                break;
            }
        }

        return "redirect:/dashboard";
    }

    @PostMapping(value = "/create-backup")
    public ResponseEntity createBackup(@Valid CreateBackupSettings createBackupSettings, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            logger.info("Has errors");
            logger.info(bindingResult.getAllErrors().toString());
        }

        return ResponseEntity.status(HttpStatus.OK).body(null);
    }
}
