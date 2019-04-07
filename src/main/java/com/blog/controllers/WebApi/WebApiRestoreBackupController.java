package com.blog.controllers.WebApi;

import com.blog.controllers.WebApi.Validator.WebRestoreBackupRequestValidator;
import com.blog.entities.backup.BackupProperties;
import com.blog.entities.database.DatabaseSettings;
import com.blog.entities.storage.StorageSettings;
import com.blog.manager.*;
import com.example.demo.manager.*;
import com.blog.webUI.formTransfer.WebRestoreBackupRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
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

    private WebRestoreBackupRequestValidator webRestoreBackupRequestValidator;

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

    @Autowired
    public void setWebRestoreBackupRequestValidator(WebRestoreBackupRequestValidator webRestoreBackupRequestValidator) {
        this.webRestoreBackupRequestValidator = webRestoreBackupRequestValidator;
    }

    @PostMapping
    public String restoreBackup(WebRestoreBackupRequest webRestoreBackupRequest, BindingResult bindingResult) {
        logger.info("restoreBackup(): Got backup restoration job");

        webRestoreBackupRequestValidator.validate(webRestoreBackupRequest, bindingResult);
        if (bindingResult.hasErrors()) {
            logger.info("Has errors: {}", bindingResult.getAllErrors());

            return "dashboard";
        }

        Integer backupId = Integer.valueOf(webRestoreBackupRequest.getBackupId());
        BackupProperties backupProperties = backupPropertiesManager.getById(backupId).orElseThrow(() ->
                new RuntimeException(String.format(
                        "Can't retrieve backup properties. Error: no backup properties with ID %d", backupId)));

        String storageSettingsName = backupProperties.getStorageSettingsName();
        StorageSettings storageSettings = storageSettingsManager.getById(storageSettingsName).
                orElseThrow(() ->
                        new RuntimeException(String.format(
                                "Can't retrieve storage settings. Error: no storage settings with name %d", storageSettingsName)));

        String databaseSettingsName = webRestoreBackupRequest.getDatabaseSettingsName();
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
