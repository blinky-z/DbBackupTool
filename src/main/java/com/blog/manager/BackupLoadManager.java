package com.blog.manager;

import com.blog.entities.backup.BackupProperties;
import com.blog.entities.storage.StorageSettings;
import com.blog.entities.storage.StorageType;
import com.blog.service.storage.DropboxStorage;
import com.blog.service.storage.FileSystemStorage;
import com.blog.service.storage.Storage;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Component
public class BackupLoadManager {
    private static final Logger logger = LoggerFactory.getLogger(BackupLoadManager.class);

    private StorageSettingsManager storageSettingsManager;

    private BackupPropertiesManager backupPropertiesManager;

    private DropboxStorage dropboxStorage;

    private FileSystemStorage fileSystemStorage;

    @Autowired
    public void setStorageSettingsManager(StorageSettingsManager storageSettingsManager) {
        this.storageSettingsManager = storageSettingsManager;
    }

    @Autowired
    public void setBackupPropertiesManager(BackupPropertiesManager backupPropertiesManager) {
        this.backupPropertiesManager = backupPropertiesManager;
    }

    @Autowired
    public void setDropboxStorage(DropboxStorage dropboxStorage) {
        this.dropboxStorage = dropboxStorage;
    }

    @Autowired
    public void setFileSystemStorage(FileSystemStorage fileSystemStorage) {
        this.fileSystemStorage = fileSystemStorage;
    }

    public BackupProperties getNewBackupProperties(@NotNull StorageSettings storageSettings, @NotNull List<String> processors,
                                                   @NotNull String databaseName) {
        Date creationTime = new Date();
        String backupName = String.format(Storage.BACKUP_NAME_TEMPLATE, databaseName, Storage.dateFormatter.format(creationTime));

        BackupProperties backupProperties = new BackupProperties(backupName, processors, creationTime, storageSettings.getSettingsName());
        return backupPropertiesManager.save(backupProperties);
    }

    public void uploadBackup(@NotNull InputStream backupStream, @NotNull BackupProperties backupProperties) {
        Objects.requireNonNull(backupStream);
        Objects.requireNonNull(backupProperties);

        String settingsName = backupProperties.getStorageSettingsName();
        StorageSettings storageSettings = storageSettingsManager.getById(settingsName).
                orElseThrow(() -> new RuntimeException(
                        String.format("Can't upload backup. Missing storage settings with name %", settingsName)));

        StorageType storageType = storageSettings.getType();
        String backupName = backupProperties.getBackupName();
        logger.info("Uploading backup to {}. Backup name: {}", storageType, backupName);
        switch (storageType) {
            case LOCAL_FILE_SYSTEM: {
                fileSystemStorage.uploadBackup(backupStream, storageSettings, backupName);
                break;
            }
            case DROPBOX: {
                dropboxStorage.uploadBackup(backupStream, storageSettings, backupName);
                break;
            }
            default: {
                throw new RuntimeException(String.format("Can't upload backup. Unknown storage type: %s", storageType));
            }
        }

        logger.info("Backup successfully uploaded to {}. Backup name: {}", storageType, backupName);
    }

    public InputStream downloadBackup(@NotNull BackupProperties backupProperties) {
        Objects.requireNonNull(backupProperties);

        logger.info("Downloading backup... Backup properties: {}", backupProperties);

        String storageSettingsName = backupProperties.getStorageSettingsName();
        StorageSettings storageSettings = storageSettingsManager.getById(storageSettingsName).orElseThrow(
                () -> new RuntimeException("Can't download backup: no such storage settings with name " + storageSettingsName));
        StorageType storageType = storageSettings.getType();

        String backupName = backupProperties.getBackupName();

        InputStream downloadedBackup;
        switch (storageType) {
            case LOCAL_FILE_SYSTEM: {
                downloadedBackup = fileSystemStorage.downloadBackup(storageSettings, backupName);
                break;
            }
            case DROPBOX: {
                downloadedBackup = dropboxStorage.downloadBackup(storageSettings, backupName);
                break;
            }
            default: {
                throw new RuntimeException(String.format("Can't download backup. Unknown storage type: %s", storageType));
            }
        }
        logger.info("Backup successfully downloaded from storage {}. Backup name: {}", storageType, backupName);

        return downloadedBackup;
    }

    public void deleteBackup(@NotNull BackupProperties backupProperties) {
        Objects.requireNonNull(backupProperties);

        logger.info("Deleting backup... Backup properties: {}", backupProperties);

        String storageSettingsName = backupProperties.getStorageSettingsName();
        StorageSettings storageSettings = storageSettingsManager.getById(storageSettingsName).orElseThrow(
                () -> new RuntimeException("Can't delete backup: no such storage settings with name " + storageSettingsName));
        StorageType storageType = storageSettings.getType();

        String backupName = backupProperties.getBackupName();

        switch (storageType) {
            case LOCAL_FILE_SYSTEM: {
                fileSystemStorage.deleteBackup(storageSettings, backupName);
                break;
            }
            case DROPBOX: {
                dropboxStorage.deleteBackup(storageSettings, backupName);
                break;
            }
            default: {
                throw new RuntimeException(String.format("Can't delete backup. Unknown storage type: %s", storageType));
            }
        }

        logger.info("Backup successfully deleted from storage {}. Backup name: {}", storageType, backupName);
    }
}