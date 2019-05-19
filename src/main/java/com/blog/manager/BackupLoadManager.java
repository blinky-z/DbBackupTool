package com.blog.manager;

import com.blog.entities.backup.BackupProperties;
import com.blog.entities.storage.StorageSettings;
import com.blog.entities.storage.StorageType;
import com.blog.service.storage.DropboxStorage;
import com.blog.service.storage.FileSystemStorage;
import com.blog.service.storage.StorageConstants;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * This manager class provides API to work with backups.
 */
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

    /**
     * Use this method to build new {@link BackupProperties} before uploading backup to storage.
     *
     * @param storageSettings storage settings where backup will be uploaded to
     * @param processors      processors that applies on backup
     * @param databaseName    database name of database of which backup was created
     * @return new BackupProperties with required fields set
     */
    public BackupProperties initNewBackupProperties(@NotNull StorageSettings storageSettings, @NotNull List<String> processors,
                                                    @NotNull String databaseName) {
        Date creationTime = new Date();
        String backupName = String.format(
                StorageConstants.BACKUP_NAME_TEMPLATE, databaseName, StorageConstants.dateFormatter.format(creationTime));

        BackupProperties backupProperties =
                new BackupProperties(backupName, processors, creationTime, storageSettings.getSettingsName());
        return backupPropertiesManager.save(backupProperties);
    }

    /**
     * Uploads backup.
     *
     * @param backupStream     InputStream from which backup can be read
     * @param backupProperties pre-created BackupProperties of backup that should be uploaded to storage
     * @param id               backup upload task ID
     */
    public void uploadBackup(@NotNull InputStream backupStream, @NotNull BackupProperties backupProperties,
                             @NotNull Integer id) {
        Objects.requireNonNull(backupStream);
        Objects.requireNonNull(backupProperties);
        Objects.requireNonNull(id);

        String settingsName = backupProperties.getStorageSettingsName();
        StorageSettings storageSettings = storageSettingsManager.getById(settingsName).
                orElseThrow(() -> new RuntimeException(
                        String.format("Can't upload backup. Missing storage settings with name %s", settingsName)));

        StorageType storageType = storageSettings.getType();
        String backupName = backupProperties.getBackupName();
        logger.info("Uploading backup to {}. Backup name: {}", storageType, backupName);

        switch (storageType) {
            case LOCAL_FILE_SYSTEM: {
                fileSystemStorage.uploadBackup(backupStream, storageSettings, backupName, id);
                break;
            }
            case DROPBOX: {
                dropboxStorage.uploadBackup(backupStream, storageSettings, backupName, id);
                break;
            }
            default: {
                throw new RuntimeException(String.format("Can't upload backup. Unknown storage type: %s", storageType));
            }
        }

        logger.info("Backup successfully uploaded to {}. Backup name: {}", storageType, backupName);
    }

    /**
     * Downloads backup.
     *
     * @param backupProperties BackupProperties of created backup
     * @param id               backup restoration task ID
     * @return InputStream from which downloaded backup can be read
     */
    public InputStream downloadBackup(@NotNull BackupProperties backupProperties, @NotNull Integer id) {
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
                downloadedBackup = fileSystemStorage.downloadBackup(storageSettings, backupName, id);
                break;
            }
            case DROPBOX: {
                downloadedBackup = dropboxStorage.downloadBackup(storageSettings, backupName, id);
                break;
            }
            default: {
                throw new RuntimeException(String.format("Can't download backup. Unknown storage type: %s", storageType));
            }
        }
        logger.info("Backup successfully downloaded from {}. Backup name: {}", storageType, backupName);

        return downloadedBackup;
    }

    /**
     * Deletes backup.
     *
     * @param backupProperties BackupProperties of created backup
     * @param id               backup deletion task ID
     */
    public void deleteBackup(@NotNull BackupProperties backupProperties, @NotNull Integer id) {
        Objects.requireNonNull(backupProperties);

        logger.info("Deleting backup... Backup properties: {}", backupProperties);

        String storageSettingsName = backupProperties.getStorageSettingsName();
        StorageSettings storageSettings = storageSettingsManager.getById(storageSettingsName).orElseThrow(
                () -> new RuntimeException("Can't delete backup: no such storage settings with name " + storageSettingsName));
        StorageType storageType = storageSettings.getType();

        String backupName = backupProperties.getBackupName();

        switch (storageType) {
            case LOCAL_FILE_SYSTEM: {
                fileSystemStorage.deleteBackup(storageSettings, backupName, id);
                break;
            }
            case DROPBOX: {
                dropboxStorage.deleteBackup(storageSettings, backupName, id);
                break;
            }
            default: {
                throw new RuntimeException(String.format("Can't delete backup. Unknown storage type: %s", storageType));
            }
        }

        logger.info("Backup successfully deleted from {}. Backup name: {}", storageType, backupName);
    }
}