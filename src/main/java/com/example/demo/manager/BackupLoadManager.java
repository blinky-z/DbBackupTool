package com.example.demo.manager;

import com.example.demo.entities.backup.BackupProperties;
import com.example.demo.entities.storage.StorageSettings;
import com.example.demo.entities.storage.StorageType;
import com.example.demo.service.storage.DropboxStorage;
import com.example.demo.service.storage.FileSystemStorage;
import com.example.demo.service.storage.Storage;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Component
public class BackupLoadManager {
    private BackupPropertiesManager backupPropertiesManager;

    private BackupProcessorManager backupProcessorManager;

    private static final String BACKUP_NAME_TEMPLATE = "backup_%s_%s";

    private static final SimpleDateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss-SS");

    @Autowired
    public void setBackupPropertiesManager(BackupPropertiesManager backupPropertiesManager) {
        this.backupPropertiesManager = backupPropertiesManager;
    }

    @Autowired
    public void setBackupProcessorManager(BackupProcessorManager backupProcessorManager) {
        this.backupProcessorManager = backupProcessorManager;
    }

    private static final Logger logger = LoggerFactory.getLogger(DatabaseBackupManager.class);

    public BackupProperties uploadBackup(@NotNull InputStream backupStream, @NotNull StorageSettings storageSettings,
                                         @NotNull List<String> processors, @NotNull String databaseName) {
        Date creationTime = new Date();
        String backupName = String.format(BACKUP_NAME_TEMPLATE, databaseName, dateFormatter.format(creationTime));

        Storage storage;
        StorageType storageType = storageSettings.getType();
        switch (storageType) {
            case LOCAL_FILE_SYSTEM: {
                storage = new FileSystemStorage(storageSettings, backupName);
                break;
            }
            case DROPBOX: {
                storage = new DropboxStorage(storageSettings, backupName);
                break;
            }
            default: {
                throw new RuntimeException(String.format("Can't upload backup. Unknown storage type: %s", storageType));
            }
        }

        logger.info("Uploading backup to {}. Backup name: {}", storageType, backupName);
        storage.uploadBackup(backupStream);

        logger.info("Backup successfully uploaded to storage {}. Backup name: {}", storageType, backupName);

        BackupProperties backupProperties = new BackupProperties(backupName, processors, creationTime, storageSettings.getId());
        backupProperties = backupPropertiesManager.save(backupProperties);

        logger.info("Uploaded backup properties saved. Backup name: {}", backupName);

        return backupProperties;
    }

    public InputStream downloadBackup(@NotNull StorageSettings storageSettings, @NotNull BackupProperties backupProperties) {
        Objects.requireNonNull(storageSettings);
        Objects.requireNonNull(backupProperties);
        StorageType storageType = storageSettings.getType();
        String backupName = backupProperties.getBackupName();
        logger.info("Downloading backup from storage {}. Backup name: {}", storageType, backupName);

        Storage storage;
        switch (storageType) {
            case LOCAL_FILE_SYSTEM: {
                storage = new FileSystemStorage(storageSettings, backupName);
                break;
            }
            case DROPBOX: {
                storage = new DropboxStorage(storageSettings, backupName);
                break;
            }
            default: {
                throw new RuntimeException(String.format("Can't download backup. Unknown storage type: %s", storageType));
            }
        }

        InputStream downloadedBackup = storage.downloadBackup();

        logger.info("Backup successfully downloaded from storage {}. Backup name: {}", storageType, backupName);

        return downloadedBackup;
    }
}
