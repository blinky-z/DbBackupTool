package com.example.demo.controllers;

import com.example.demo.entities.backup.BackupProperties;
import com.example.demo.manager.*;
import com.example.demo.entities.database.Database;
import com.example.demo.entities.database.DatabaseSettings;
import com.example.demo.entities.database.PostgresSettings;
import com.example.demo.entities.storage.DropboxSettings;
import com.example.demo.entities.storage.LocalFileSystemSettings;
import com.example.demo.entities.storage.Storage;
import com.example.demo.entities.storage.StorageSettings;
import com.example.demo.service.processor.BackupCompressor;
import com.example.demo.webUI.formTransfer.WebCreateBackupRequest;
import com.example.demo.webUI.formTransfer.WebAddDatabaseRequest;
import com.example.demo.webUI.formTransfer.WebAddStorageRequest;
import com.example.demo.webUI.formTransfer.WebRestoreBackupRequest;
import com.example.demo.webUI.formTransfer.storage.WebDropboxSettings;
import com.example.demo.webUI.formTransfer.storage.WebLocalFileSystemSettings;
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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Controller
public class ApiController {
    private static final Logger logger = LoggerFactory.getLogger(ApiController.class);

    private DatabaseSettingsManager databaseSettingsManager;

    private StorageSettingsManager storageSettingsManager;

    private DatabaseBackupManager databaseBackupManager;

    private TextStorageBackupLoadManager textStorageBackupLoadManager;

    private BinaryStorageBackupLoadManager binaryStorageBackupLoadManager;

    private BackupPropertiesManager backupPropertiesManager;

    private BackupCompressor backupCompressor;

    @Autowired
    public void setDatabaseSettingsManager(DatabaseSettingsManager databaseSettingsManager) {
        this.databaseSettingsManager = databaseSettingsManager;
    }

    @Autowired
    public void setStorageSettingsManager(StorageSettingsManager storageSettingsManager) {
        this.storageSettingsManager = storageSettingsManager;
    }

    @Autowired
    public void setDatabaseBackupManager(DatabaseBackupManager databaseBackupManager) {
        this.databaseBackupManager = databaseBackupManager;
    }

    @Autowired
    public void setTextStorageBackupLoadManager(TextStorageBackupLoadManager textStorageBackupLoadManager) {
        this.textStorageBackupLoadManager = textStorageBackupLoadManager;
    }

    @Autowired
    public void setBinaryStorageBackupLoadManager(BinaryStorageBackupLoadManager binaryStorageBackupLoadManager) {
        this.binaryStorageBackupLoadManager = binaryStorageBackupLoadManager;
    }

    @Autowired
    public void setBackupPropertiesManager(BackupPropertiesManager backupPropertiesManager) {
        this.backupPropertiesManager = backupPropertiesManager;
    }

    @Autowired
    public void setBackupCompressor(BackupCompressor backupCompressor) {
        this.backupCompressor = backupCompressor;
    }

    //    TODO: добавить нормальную валидацию всех форм.

    @DeleteMapping(value = "/database")
    public String deleteDatabase(@RequestParam(value = "id") int id) {
        logger.info("Deletion of database: id: {}", id);

        databaseSettingsManager.deleteById(id);

        return "redirect:/dashboard";
    }

    @PostMapping(value = "/database")
    public String createDatabase(@Valid WebAddDatabaseRequest createDatabaseRequest, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            logger.info("Has errors");
            logger.info(bindingResult.getAllErrors().toString());
        }

        Optional<Database> databaseType = Database.of(createDatabaseRequest.getDatabaseType());
        if (databaseType.isPresent()) {
            switch (databaseType.get()) {
                case POSTGRES: {
                    PostgresSettings postgresSettings = new PostgresSettings();
//                    WebPostgresSettings webPostgresSettings = Objects.requireNonNull(createDatabaseRequest.
//                    getPostgresSettings());

                    DatabaseSettings databaseSettings = DatabaseSettings.postgresSettings(postgresSettings)
                            .withHost(createDatabaseRequest.getHost())
                            .withPort(createDatabaseRequest.getPort())
                            .withName(createDatabaseRequest.getName())
                            .withLogin(createDatabaseRequest.getLogin())
                            .withPassword(createDatabaseRequest.getPassword())
                            .build();
                    databaseSettingsManager.save(databaseSettings);
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

        storageSettingsManager.deleteById(id);

        return "redirect:/dashboard";
    }

    @PostMapping(value = "/storage")
    public String createStorage(@Valid WebAddStorageRequest createStorageRequest, BindingResult bindingResult) {
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
                    storageSettingsManager.save(storageSettings);
                    break;
                }
                case LOCAL_FILE_SYSTEM: {
                    LocalFileSystemSettings localFileSystemSettings = new LocalFileSystemSettings();
                    WebLocalFileSystemSettings webLocalFileSystemSettings = Objects.requireNonNull(
                            createStorageRequest.getLocalFileSystemSettings());

                    localFileSystemSettings.setBackupPath(webLocalFileSystemSettings.getBackupPath());

                    StorageSettings storageSettings = StorageSettings.localFileSystemSettings(localFileSystemSettings).build();
                    storageSettingsManager.save(storageSettings);
                    break;
                }
            }
        } else {
            throw new RuntimeException("Can't create storage configuration. Error: Unknown storage type");
        }

        return "redirect:/dashboard";
    }

    @PostMapping(value = "/create-backup")
    public ResponseEntity createBackup(WebCreateBackupRequest createBackupRequest) {
        logger.info("{}", createBackupRequest.getCheckStorageList());
        logger.info("{}", createBackupRequest.getCheckDatabaseList());

        List<DatabaseSettings> databaseSettingsList = new ArrayList<>();
        for (Integer databaseId : createBackupRequest.getCheckDatabaseList()) {
            databaseSettingsList.add(databaseSettingsManager.getById(databaseId).orElseThrow(() -> new RuntimeException(
                    String.format("Can't retrieve database settings. Error: no database settings with ID %d", databaseId))));
        }

        List<StorageSettings> storageSettingsList = new ArrayList<>();
        for (Integer storageId : createBackupRequest.getCheckStorageList()) {
            storageSettingsList.add(storageSettingsManager.getById(storageId).orElseThrow(() -> new RuntimeException(
                    String.format("Can't retrieve storage settings. Error: no storage settings with ID %d", storageId))));
        }

        logger.info("Database settings list: {}", databaseSettingsList);
        logger.info("Storage settings list: {}", storageSettingsList);

        if (createBackupRequest.isCompress()) {
            for (DatabaseSettings currentDatabaseSettings : databaseSettingsList) {
                InputStream backupStream = databaseBackupManager.createBackup(currentDatabaseSettings);
                InputStream compressedBackupStream = backupCompressor.compressBackup(backupStream);

                binaryStorageBackupLoadManager.uploadBackup(compressedBackupStream, storageSettingsList,
                        currentDatabaseSettings.getName(), createBackupRequest.getMaxChunkSize());
            }
        } else {
            for (DatabaseSettings currentDatabaseSettings : databaseSettingsList) {
                InputStream currentBackup = databaseBackupManager.createBackup(currentDatabaseSettings);

                textStorageBackupLoadManager.uploadBackup(currentBackup, storageSettingsList,
                        currentDatabaseSettings.getName(), createBackupRequest.getMaxChunkSize());
            }
        }


        return ResponseEntity.status(HttpStatus.OK).body(null);
    }

    @PostMapping(value = "/restore-backup")
    public ResponseEntity restoreBackup(WebRestoreBackupRequest restoreBackupRequest) {
        logger.info("Backup id: {}", restoreBackupRequest.getBackupId());
        logger.info("Database id: {}", restoreBackupRequest.getDatabaseId());

        Integer backupId = restoreBackupRequest.getBackupId();
        BackupProperties backupProperties = backupPropertiesManager.getById(backupId).orElseThrow(() ->
                new RuntimeException(String.format(
                        "Can't retrieve backup properties. Error: no backup properties with ID %d", backupId)));

        Integer storageSettingsId = backupProperties.getStorageSettingsId();
        StorageSettings storageSettings = storageSettingsManager.getById(storageSettingsId).
                orElseThrow(() ->
                        new RuntimeException(String.format(
                                "Can't retrieve storage settings. Error: no storage settings with ID %d", storageSettingsId)));

        Integer databaseId = restoreBackupRequest.getDatabaseId();
        DatabaseSettings databaseSettings = databaseSettingsManager.getById(databaseId).orElseThrow(() -> new RuntimeException(
                String.format("Can't retrieve database settings. Error: no database settings with ID %d", databaseId)));

        logger.info("Backup properties: {}", backupProperties);
        logger.info("Database settings: {}", databaseSettings);

        if (backupProperties.isCompressed()) {
            InputStream downloadedBackup = textStorageBackupLoadManager.downloadBackup(storageSettings,
                    backupProperties);
            InputStream decompressedBackup = backupCompressor.decompressBackup(downloadedBackup);

            databaseBackupManager.restoreBackup(decompressedBackup, databaseSettings);
        } else {
            InputStream downloadedBackup = textStorageBackupLoadManager.downloadBackup(storageSettings,
                    backupProperties);
            databaseBackupManager.restoreBackup(downloadedBackup, databaseSettings);
        }

        return ResponseEntity.status(HttpStatus.OK).body(null);
    }
}
