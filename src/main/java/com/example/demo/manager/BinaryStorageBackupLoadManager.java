package com.example.demo.manager;

import com.example.demo.entities.backup.BackupProperties;
import com.example.demo.entities.storage.Storage;
import com.example.demo.entities.storage.StorageSettings;
import com.example.demo.service.storage.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Component
public class BinaryStorageBackupLoadManager {
    private BackupPropertiesManager backupPropertiesManager;
    private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss-SS");

    @Autowired
    public void setBackupPropertiesManager(BackupPropertiesManager backupPropertiesManager) {
        this.backupPropertiesManager = backupPropertiesManager;
    }

    private static final Logger logger = LoggerFactory.getLogger(DatabaseBackupManager.class);

    public List<BackupProperties> uploadBackup(InputStream backupStream, List<StorageSettings> storageSettingsList,
                                               String databaseName, int maxChunkSize) {
        List<BinaryStorage> storageList = new ArrayList<>();
        Date creationTime = new Date();
        String backupName = String.format("backup_%s_%s", databaseName, DATE_FORMATTER.format(creationTime));
        logger.info("Uploading compressed backup to {} storages. Backup name: {}", storageSettingsList.size());
        for (StorageSettings storageSettings : storageSettingsList) {
            Storage storageType = storageSettings.getType();
            switch (storageType) {
                case LOCAL_FILE_SYSTEM: {
                    storageList.add(new FileSystemBinaryStorage(storageSettings, backupName));
                    break;
                }
                case DROPBOX: {
                    storageList.add(new DropboxBinaryStorage(storageSettings, backupName));
                    break;
                }
                default: {
                    logger.error("Can't upload compressed backup to current storage. Unknown storage type: {}. " +
                            "Skipping this storage for uploading", storageType);
                }
            }
        }

        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

            final byte[] buffer = new byte[8 * 1024];
            int bytesRead;
            int totalBytes = 0;
            int currentChunkSize = 0;
            while ((bytesRead = backupStream.read(buffer)) != -1) {
                currentChunkSize += bytesRead;
                byteArrayOutputStream.write(buffer, 0, bytesRead);

                if (currentChunkSize >= maxChunkSize) {
                    for (BinaryStorage currentStorage : storageList) {
                        currentStorage.uploadBackup(byteArrayOutputStream.toByteArray());
                    }
                    totalBytes += currentChunkSize;
                    byteArrayOutputStream.reset();
                    currentChunkSize = 0;
                }
            }
            if (currentChunkSize != 0) {
                for (BinaryStorage currentStorage : storageList) {
                    currentStorage.uploadBackup(byteArrayOutputStream.toByteArray());
                }
                totalBytes += currentChunkSize;
            }
            logger.info("Compressed backup uploading completed. Backup name: {}. Total bytes written: {}", backupName, totalBytes);
        } catch (IOException ex) {
            throw new RuntimeException("Error occurred while reading compressed backup to upload", ex);
        }

        List<BackupProperties> backupPropertiesList = new ArrayList<>();
        for (StorageSettings storageSettings : storageSettingsList) {
            BackupProperties backupProperties = new BackupProperties(backupName, true, creationTime,
                    storageSettings.getId());
            backupPropertiesList.add(backupPropertiesManager.save(backupProperties));
        }

        return backupPropertiesList;
    }

    public InputStream downloadBackup(@NotNull StorageSettings storageSettings, @NotNull BackupProperties backupProperties) {
        Objects.requireNonNull(storageSettings);
        Objects.requireNonNull(backupProperties);
        Storage storageType = storageSettings.getType();
        String backupName = backupProperties.getBackupName();
        logger.info("Downloading compressed backup from Storage {}. Backup name: {}...", storageType, backupName);

        InputStream downloadedBackup;

        switch (storageType) {
            case LOCAL_FILE_SYSTEM: {
                FileSystemBinaryStorage fileSystemBinaryStorage = new FileSystemBinaryStorage(storageSettings, backupName);
                downloadedBackup = fileSystemBinaryStorage.downloadBackup();
                break;
            }
            case DROPBOX: {
                DropboxBinaryStorage dropboxBinaryStorage = new DropboxBinaryStorage(storageSettings, backupName);
                downloadedBackup =  dropboxBinaryStorage.downloadBackup();
                break;
            }
            default: {
                throw new RuntimeException(String.format("Can't download compressed backup. Unknown storage type: %s", storageType));
            }
        }

        logger.info("Compressed backup successfully downloaded from Storage {}. Backup name: {}", storageType, backupName);

        return downloadedBackup;
    }
}
