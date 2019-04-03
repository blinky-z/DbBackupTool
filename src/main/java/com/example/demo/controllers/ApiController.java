package com.example.demo.controllers;

import com.example.demo.entities.backup.BackupProperties;
import com.example.demo.entities.database.Database;
import com.example.demo.entities.database.DatabaseSettings;
import com.example.demo.entities.database.PostgresSettings;
import com.example.demo.entities.storage.DropboxSettings;
import com.example.demo.entities.storage.LocalFileSystemSettings;
import com.example.demo.entities.storage.StorageSettings;
import com.example.demo.entities.storage.StorageType;
import com.example.demo.manager.*;
import com.example.demo.webUI.formTransfer.WebAddDatabaseRequest;
import com.example.demo.webUI.formTransfer.WebAddStorageRequest;
import com.example.demo.webUI.formTransfer.WebCreateBackupRequest;
import com.example.demo.webUI.formTransfer.WebRestoreBackupRequest;
import com.example.demo.webUI.formTransfer.storage.WebDropboxSettings;
import com.example.demo.webUI.formTransfer.storage.WebLocalFileSystemSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.validation.Valid;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Controller
public class ApiController {
    private static final Logger logger = LoggerFactory.getLogger(ApiController.class);

    private DatabaseSettingsManager databaseSettingsManager;

    private StorageSettingsManager storageSettingsManager;

    private DatabaseBackupManager databaseBackupManager;

    private BackupLoadManager backupLoadManager;

    private BackupPropertiesManager backupPropertiesManager;

    private BackupProcessorManager backupProcessorManager;

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
    public void setBackupLoadManager(BackupLoadManager backupLoadManager) {
        this.backupLoadManager = backupLoadManager;
    }

    @Autowired
    public void setBackupPropertiesManager(BackupPropertiesManager backupPropertiesManager) {
        this.backupPropertiesManager = backupPropertiesManager;
    }

    @Autowired
    public void setBackupProcessorManager(BackupProcessorManager backupProcessorManager) {
        this.backupProcessorManager = backupProcessorManager;
    }

    @DeleteMapping(value = "/database")
    public String deleteDatabase(@RequestParam(value = "id") int id) {
        logger.info("deleteDatabase(): Got database configuration deletion job. Database ID: {}", id);

        databaseSettingsManager.deleteById(id);

        return "redirect:/dashboard";
    }

    @PostMapping(value = "/database")
    public String createDatabase(@Valid WebAddDatabaseRequest createDatabaseRequest) {
        logger.info("createDatabase(): Got database configuration creation job");

        Optional<Database> databaseType = Database.of(createDatabaseRequest.getDatabaseType());
        if (databaseType.isPresent()) {
            switch (databaseType.get()) {
                case POSTGRES: {
                    PostgresSettings postgresSettings = new PostgresSettings();

                    DatabaseSettings databaseSettings = DatabaseSettings.postgresSettings(postgresSettings)
                            .withHost(createDatabaseRequest.getHost())
                            .withPort(Integer.valueOf(createDatabaseRequest.getPort()))
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
        logger.info("deleteStorage(): Got storage configuration deletion job. StorageType ID: {}", id);

        storageSettingsManager.deleteById(id);

        return "redirect:/dashboard";
    }

    @PostMapping(value = "/storage")
    public String createStorage(WebAddStorageRequest createStorageRequest) {
        logger.info("createStorage(): Got storage configuration creation job");

        Optional<StorageType> storageType = StorageType.of(createStorageRequest.getStorageType());
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
        logger.info("createBackup(): Got backup creation job");

        int databaseId = createBackupRequest.getDatabaseId();
        DatabaseSettings databaseSettings = databaseSettingsManager.getById(databaseId).orElseThrow(() -> new RuntimeException(
                String.format("Can't retrieve database settings. Error: no database settings with ID %d", databaseId)));
        String databaseName = databaseSettings.getName();

        logger.info("createBackup(): Database settings: {}", databaseSettings);

        for (WebCreateBackupRequest.StorageProperties storageProperties : createBackupRequest.getStorageProperties()) {
            int storageId = storageProperties.getId();
            StorageSettings storageSettings = storageSettingsManager.getById(storageId).orElseThrow(() -> new RuntimeException(
                    String.format("createBackup(): Can't retrieve storage settings. Error: no storage settings with ID %d",
                            storageId)));

            logger.info("Current storage settings: {}", storageSettings.toString());

            logger.info("createBackup(): Creating backup...");
            InputStream backupStream = databaseBackupManager.createBackup(databaseSettings);

            List<String> processorList = storageProperties.getProcessors();
            logger.info("createBackup(): Applying processors on created backup. Processors: {}", processorList);
            InputStream processedBackupStream = backupProcessorManager.process(backupStream, processorList);

            logger.info("createBackup(): Uploading backup...");
            backupLoadManager.uploadBackup(processedBackupStream, storageSettings, processorList, databaseName);
        }

        return ResponseEntity.status(HttpStatus.OK).body(null);
    }

    @PostMapping(value = "/restore-backup")
    public ResponseEntity restoreBackup(WebRestoreBackupRequest restoreBackupRequest) {
        logger.info("restoreBackup(): Got backup restoration job. Backup id: {}. Database id: {}",
                restoreBackupRequest.getBackupId(), restoreBackupRequest.getDatabaseId());

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

        logger.info("restoreBackup(): Backup properties: {}", backupProperties);
        logger.info("restoreBackup(): Database settings: {}", databaseSettings);


        logger.info("restoreBackup(): Downloading backup...");
        InputStream downloadedBackup = backupLoadManager.downloadBackup(storageSettings, backupProperties);

        logger.info("restoreBackup(): Deprocessing backup...");
        InputStream deprocessedBackup = backupProcessorManager.deprocess(downloadedBackup, backupProperties.getProcessors());

        logger.info("restoreBackup(): Restoring backup...");
        databaseBackupManager.restoreBackup(deprocessedBackup, databaseSettings);

        return ResponseEntity.status(HttpStatus.OK).body(null);
    }
}
