package com.example.demo.manager;

import com.example.demo.entities.backup.BackupProperties;
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

@Component
public class TextStorageBackupLoadManager {
    private BackupPropertiesManager backupPropertiesManager;
    private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss");

    @Autowired
    public void setBackupPropertiesManager(BackupPropertiesManager backupPropertiesManager) {
        this.backupPropertiesManager = backupPropertiesManager;
    }

    private static final Logger logger = LoggerFactory.getLogger(DatabaseBackupManager.class);

    public List<BackupProperties> uploadBackup(InputStream backupStream, List<StorageSettings> storageSettingsList,
                                               String databaseName, int maxChunkSize) {
        List<TextStorage> storageList = new ArrayList<>();
        Date creationTime = new Date();
        String backupName = String.format("backup_%s_%s", databaseName, SIMPLE_DATE_FORMAT.format(creationTime));
        for (StorageSettings storageSettings : storageSettingsList) {
            switch (storageSettings.getType()) {
                case LOCAL_FILE_SYSTEM: {
                    storageList.add(new FileSystemTextStorage(storageSettings, backupName));
                    break;
                }
                case DROPBOX: {
                    storageList.add(new DropboxTextStorage(storageSettings, backupName));
                    break;
                }
                default: {
                    throw new RuntimeException("Can't upload backup: Unknown storage type");
                }
            }
        }

        try (
                BufferedReader backupReader = new BufferedReader(new InputStreamReader(backupStream));
        ) {
            StringBuilder currentChunk = new StringBuilder();
            long currentChunkSize = 0;
            String currentLine;
            while ((currentLine = backupReader.readLine()) != null) {
                currentChunk.append(currentLine).append(System.lineSeparator());
                currentChunkSize += currentLine.getBytes().length;

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
            }
        } catch (IOException ex) {
            throw new RuntimeException("Error occurred while reading backup", ex);
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
        String backupName = backupProperties.getBackupName();
        switch (storageSettings.getType()) {
            case LOCAL_FILE_SYSTEM: {
                FileSystemTextStorage fileSystemTextStorage = new FileSystemTextStorage(storageSettings, backupName);
                return fileSystemTextStorage.downloadBackup();
            }
            case DROPBOX: {
                DropboxTextStorage dropboxTextStorage = new DropboxTextStorage(storageSettings, backupName);
                return dropboxTextStorage.downloadBackup();
            }
            default: {
                throw new RuntimeException("Can't download backup: Unknown storage type");
            }
        }
    }
}
