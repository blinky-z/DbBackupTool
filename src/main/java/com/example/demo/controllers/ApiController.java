package com.example.demo.controllers;

import com.example.demo.DatabaseManager.DatabaseManager;
import com.example.demo.DbBackup;
import com.example.demo.StorageManager.StorageManager;
import com.example.demo.entities.database.Database;
import com.example.demo.entities.database.DatabaseSettings;
import com.example.demo.entities.database.PostgresSettings;
import com.example.demo.entities.storage.DropboxSettings;
import com.example.demo.entities.storage.LocalFileSystemSettings;
import com.example.demo.entities.storage.Storage;
import com.example.demo.entities.storage.StorageSettings;
import com.example.demo.webUi.WebUiForm.WebCreateBackupRequest;
import com.example.demo.webUi.WebUiForm.WebCreateDatabaseRequest;
import com.example.demo.webUi.WebUiForm.WebCreateStorageRequest;
import com.example.demo.webUi.WebUiForm.storage.WebDropboxSettings;
import com.example.demo.webUi.WebUiForm.storage.WebLocalFileSystemSettings;
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
import java.util.Objects;
import java.util.Optional;

@Controller
public class ApiController {
    private static final Logger logger = LoggerFactory.getLogger(DbBackup.class);

    private DatabaseManager databaseManager;

    private StorageManager storageManager;

    @Autowired
    public void setDatabaseManager(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Autowired
    public void setStorageManager(StorageManager storageManager) {
        this.storageManager = storageManager;
    }

//    TODO: добавить нормальную валидацию всех форм.

    @DeleteMapping(value = "/database")
    public String deleteDatabase(@RequestParam(value = "id") int id) {
        logger.info("Deletion of database: id: {}", id);

        databaseManager.deleteDatabaseSettings(id);

        return "redirect:/dashboard";
    }

    @PostMapping(value = "/database")
    public String createDatabase(@Valid WebCreateDatabaseRequest createDatabaseRequest, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            logger.info("Has errors");
            logger.info(bindingResult.getAllErrors().toString());
        }

        Optional<Database> databaseType = Database.of(createDatabaseRequest.getDatabaseType());
        if (databaseType.isPresent()) {
            switch (databaseType.get()) {
                case POSTGRES: {
                    PostgresSettings postgresSettings = new PostgresSettings();
//                    WebPostgresSettings webPostgresSettings = Objects.requireNonNull(createDatabaseRequest.getPostgresSettings());

                    DatabaseSettings databaseSettings = DatabaseSettings.postgresSettings(postgresSettings)
                            .withHost(createDatabaseRequest.getHost())
                            .withPort(createDatabaseRequest.getPort())
                            .withName(createDatabaseRequest.getName())
                            .withLogin(createDatabaseRequest.getLogin())
                            .withPassword(createDatabaseRequest.getPassword())
                            .build();
                    databaseManager.saveDatabaseSettings(databaseSettings);
                    break;
                }
            }
        } else {
            throw new RuntimeException("Can't create database configuration. Error: Unknown database type");
        }


        return "redirect:/dashboard";
    }

    @DeleteMapping(value = "/storage")
    public String deleteStorage(@RequestParam(value = "id") int id) {
        logger.info("Deletion of storage: id: {}", id);

        storageManager.deleteStorageSettings(id);

        return "redirect:/dashboard";
    }

    @PostMapping(value = "/storage")
    public String createStorage(@Valid WebCreateStorageRequest createStorageRequest, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            logger.info("Has errors");
            logger.info(bindingResult.getAllErrors().toString());
        }

        Optional<Storage> storageType = Storage.of(createStorageRequest.getStorageType());
        if (storageType.isPresent()) {
            switch (storageType.get()) {
                case DROPBOX: {
                    DropboxSettings dropboxSettings = new DropboxSettings();
                    WebDropboxSettings webDropboxSettings = Objects.requireNonNull(createStorageRequest.getDropboxSettings());

                    dropboxSettings.setAccessToken(webDropboxSettings.getAccessToken());

                    StorageSettings storageSettings = StorageSettings.dropboxSettings(dropboxSettings).build();
                    storageManager.saveStorageSettings(storageSettings);
                    break;
                }
                case LOCAL_FILE_SYSTEM: {
                    LocalFileSystemSettings localFileSystemSettings = new LocalFileSystemSettings();
                    WebLocalFileSystemSettings webLocalFileSystemSettings = Objects.requireNonNull(
                            createStorageRequest.getLocalFileSystemSettings());

                    localFileSystemSettings.setBackupPath(webLocalFileSystemSettings.getBackupPath());

                    StorageSettings storageSettings = StorageSettings.localFileSystemSettings(localFileSystemSettings).build();
                    storageManager.saveStorageSettings(storageSettings);
                    break;
                }
            }
        } else {
            throw new RuntimeException("Can't create storage configuration. Error: Unknown storage type");
        }

        return "redirect:/dashboard";
    }

    @PostMapping(value = "/create-backup")
    public ResponseEntity createBackup(@Valid WebCreateBackupRequest createBackupRequest, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            logger.info("Has errors");
            logger.info(bindingResult.getAllErrors().toString());
        }

        return ResponseEntity.status(HttpStatus.OK).body(null);
    }
}
