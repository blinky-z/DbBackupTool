package com.example.demo.controllers.WebApi;

import com.example.demo.controllers.WebApi.Errors.ValidationError;
import com.example.demo.entities.backup.BackupProperties;
import com.example.demo.entities.database.DatabaseSettings;
import com.example.demo.entities.storage.StorageSettings;
import com.example.demo.manager.*;
import com.example.demo.webUI.formTransfer.WebRestoreBackupRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.InputStream;

@Controller
@RequestMapping("/restore-backup")
public class WebApiRestoreBackupController {
    private static final Logger logger = LoggerFactory.getLogger(WebApiRestoreBackupController.class);

    private DatabaseBackupManager databaseBackupManager;

    private BackupPropertiesManager backupPropertiesManager;

    private StorageSettingsManager storageSettingsManager;

    private DatabaseSettingsManager databaseSettingsManager;

    private BackupLoadManager backupLoadManager;

    private BackupProcessorManager backupProcessorManager;

    @Autowired
    public void setDatabaseBackupManager(DatabaseBackupManager databaseBackupManager) {
        this.databaseBackupManager = databaseBackupManager;
    }

    @Autowired
    public void setBackupPropertiesManager(BackupPropertiesManager backupPropertiesManager) {
        this.backupPropertiesManager = backupPropertiesManager;
    }

    @Autowired
    public void setStorageSettingsManager(StorageSettingsManager storageSettingsManager) {
        this.storageSettingsManager = storageSettingsManager;
    }

    @Autowired
    public void setDatabaseSettingsManager(DatabaseSettingsManager databaseSettingsManager) {
        this.databaseSettingsManager = databaseSettingsManager;
    }

    @Autowired
    public void setBackupLoadManager(BackupLoadManager backupLoadManager) {
        this.backupLoadManager = backupLoadManager;
    }

    @Autowired
    public void setBackupProcessorManager(BackupProcessorManager backupProcessorManager) {
        this.backupProcessorManager = backupProcessorManager;
    }

    private String validateRestoreBackupRequest(WebRestoreBackupRequest restoreBackupRequest) {
        if (restoreBackupRequest.getBackupId() == null) {
            return "Please, provide backup to restore";
        }
        if (restoreBackupRequest.getDatabaseSettingsName() == null) {
            return "Please, provide database to restore backup to";
        }

        return "";
    }

    @PostMapping
    public String restoreBackup(WebRestoreBackupRequest restoreBackupRequest) {
        logger.info("restoreBackup(): Got backup restoration job");

        String error = validateRestoreBackupRequest(restoreBackupRequest);
        if (!error.isEmpty()) {
            throw new ValidationError(error);
        }

        Integer backupId = restoreBackupRequest.getBackupId();
        BackupProperties backupProperties = backupPropertiesManager.getById(backupId).orElseThrow(() ->
                new RuntimeException(String.format(
                        "Can't retrieve backup properties. Error: no backup properties with ID %d", backupId)));

        String storageSettingsName = backupProperties.getStorageSettingsName();
        StorageSettings storageSettings = storageSettingsManager.getById(storageSettingsName).
                orElseThrow(() ->
                        new RuntimeException(String.format(
                                "Can't retrieve storage settings. Error: no storage settings with name %d", storageSettingsName)));

        String databaseSettingsName = restoreBackupRequest.getDatabaseSettingsName();
        DatabaseSettings databaseSettings = databaseSettingsManager.getById(databaseSettingsName).orElseThrow(() ->
                new RuntimeException(
                        String.format("Can't retrieve database settings. Error: no database settings with name %d", databaseSettingsName)));

        logger.info("restoreBackup(): Backup properties: {}", backupProperties);
        logger.info("restoreBackup(): Database settings: {}", databaseSettings);

        logger.info("restoreBackup(): Downloading backup...");
        InputStream downloadedBackup = backupLoadManager.downloadBackup(storageSettings, backupProperties);

        logger.info("restoreBackup(): Deprocessing backup...");
        InputStream deprocessedBackup = backupProcessorManager.deprocess(downloadedBackup, backupProperties.getProcessors());

        logger.info("restoreBackup(): Restoring backup...");
        databaseBackupManager.restoreBackup(deprocessedBackup, databaseSettings);

        return "redirect:/dashboard";
    }
}
