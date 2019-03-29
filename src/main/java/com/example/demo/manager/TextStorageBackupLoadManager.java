package com.example.demo.manager;

import com.example.demo.entities.backup.BackupProperties;
import com.example.demo.entities.storage.Storage;
import com.example.demo.entities.storage.StorageSettings;
import com.example.demo.service.storage.DropboxTextStorage;
import com.example.demo.service.storage.FileSystemTextStorage;
import com.example.demo.service.storage.TextStorage;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Component
public class TextStorageBackupLoadManager {
    private BackupPropertiesManager backupPropertiesManager;

    private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss-SS");

    @Autowired
    public void setBackupPropertiesManager(BackupPropertiesManager backupPropertiesManager) {
        this.backupPropertiesManager = backupPropertiesManager;
    }

    private static final Logger logger = LoggerFactory.getLogger(DatabaseBackupManager.class);

    public List<BackupProperties> uploadBackup(InputStream backupStream, List<StorageSettings> storageSettingsList,
                                               String databaseName, int maxChunkSize) {
        List<TextStorage> storageList = new ArrayList<>();
        Date creationTime = new Date();
        String backupName = String.format("backup_%s_%s", databaseName, DATE_FORMATTER.format(creationTime));
        logger.info("Uploading plain-text backup to {} storages. Backup name: {}", storageSettingsList.size());
        for (StorageSettings storageSettings : storageSettingsList) {
            Storage storageType = storageSettings.getType();
            switch (storageType) {
                case LOCAL_FILE_SYSTEM: {
                    storageList.add(new FileSystemTextStorage(storageSettings, backupName));
                    break;
                }
                case DROPBOX: {
                    storageList.add(new DropboxTextStorage(storageSettings, backupName));
                    break;
                }
                default: {
                    logger.error("Can't upload compressed backup to current storage. Unknown storage type: {}" +
                            "Skipping this storage for uploading", storageType);
                }
            }
        }

        try (
                BufferedReader backupReader = new BufferedReader(new InputStreamReader(backupStream))
        ) {
            StringBuilder currentChunk = new StringBuilder();
            long currentChunkSize = 0;
            String currentLine;
            int totalBytes = 0;
            while ((currentLine = backupReader.readLine()) != null) {
                currentChunk.append(currentLine).append(System.lineSeparator());
                currentChunkSize += currentLine.getBytes().length + System.lineSeparator().getBytes().length;
                totalBytes += currentChunkSize;

                if (currentChunkSize >= maxChunkSize) {
                    for (TextStorage currentStorage : storageList) {
                        currentStorage.uploadBackup(currentChunk.toString());
                    }
                    currentChunk.setLength(0);
                    currentChunkSize = 0;
                }
            }
            if (currentChunkSize != 0) {
                for (TextStorage currentStorage : storageList) {
                    currentStorage.uploadBackup(currentChunk.toString());
                }
                totalBytes += currentChunkSize;
            }
            logger.info("Plain text backup uploading completed. Backup name: {}. Total bytes written: {}", backupName, totalBytes);
        } catch (IOException ex) {
            throw new RuntimeException("Error occurred while reading plain-text backup to upload", ex);
        }

        List<BackupProperties> backupPropertiesList = new ArrayList<>();
        for (StorageSettings storageSettings : storageSettingsList) {
            BackupProperties backupProperties = new BackupProperties(backupName, false, creationTime,
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
        logger.info("Downloading plain-text backup from Storage {}. Backup name: {}...", storageType, backupName);

        InputStream downloadedBackup;
        switch (storageType) {
            case LOCAL_FILE_SYSTEM: {
                FileSystemTextStorage fileSystemTextStorage = new FileSystemTextStorage(storageSettings, backupName);
                downloadedBackup = fileSystemTextStorage.downloadBackup();
                break;
            }
            case DROPBOX: {
                DropboxTextStorage dropboxTextStorage = new DropboxTextStorage(storageSettings, backupName);
                downloadedBackup = dropboxTextStorage.downloadBackup();
                break;
            }
            default: {
                throw new RuntimeException(String.format("Can't download plain-text backup. Unknown storage type: %s", storageType));
            }
        }

        logger.info("Plain-text backup successfully downloaded from Storage {}. Backup name: {}", storageType, backupName);

        return downloadedBackup;
    }
}
