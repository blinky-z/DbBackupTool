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
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Component
public class BackupLoadManager {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseBackupManager.class);
    private BackupPropertiesManager backupPropertiesManager;
    private DropboxStorage dropboxStorage;
    private FileSystemStorage fileSystemStorage;

    @Autowired
    public void setDropboxStorage(DropboxStorage dropboxStorage) {
        this.dropboxStorage = dropboxStorage;
    }

    @Autowired
    public void setFileSystemStorage(FileSystemStorage fileSystemStorage) {
        this.fileSystemStorage = fileSystemStorage;
    }

    @Autowired
    public void setBackupPropertiesManager(BackupPropertiesManager backupPropertiesManager) {
        this.backupPropertiesManager = backupPropertiesManager;
    }

    public BackupProperties uploadBackup(@NotNull InputStream backupStream, @NotNull StorageSettings storageSettings,
                                         @NotNull List<String> processors, @NotNull String databaseName) {
        Date creationTime = new Date();
        String backupName = String.format(Storage.BACKUP_NAME_TEMPLATE, databaseName, Storage.dateFormatter.format(creationTime));
        StorageType storageType = storageSettings.getType();

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

        logger.info("Backup successfully uploaded to storage {}. Backup name: {}", storageType, backupName);

        BackupProperties backupProperties = new BackupProperties(backupName, processors, creationTime,
                storageSettings.getSettingsName());
        backupProperties = backupPropertiesManager.save(backupProperties);

        logger.info("Uploaded backup properties saved. Backup name: {}", backupName);

        return backupProperties;
    }

    public InputStream downloadBackup(@NotNull StorageSettings storageSettings, @NotNull BackupProperties backupProperties) {
        Objects.requireNonNull(storageSettings);
        Objects.requireNonNull(backupProperties);
        String backupName = backupProperties.getBackupName();
        StorageType storageType = storageSettings.getType();

        logger.info("Downloading backup from storage {}. Backup name: {}", storageType, backupName);
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
}
