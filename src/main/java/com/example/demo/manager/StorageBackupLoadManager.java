package com.example.demo.manager;

import com.example.demo.entities.backup.BackupProperties;
import com.example.demo.entities.database.DatabaseSettings;
import com.example.demo.entities.storage.StorageSettings;
import com.example.demo.service.storage.DropboxTextStorage;
import com.example.demo.service.storage.FileSystemTextStorage;
import com.example.demo.service.storage.TextStorage;
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
public class StorageBackupLoadManager {
    private BackupPropertiesManager backupPropertiesManager;

    @Autowired
    public void setBackupPropertiesManager(BackupPropertiesManager backupPropertiesManager) {
        this.backupPropertiesManager = backupPropertiesManager;
    }

    private static final Logger logger = LoggerFactory.getLogger(DatabaseBackupManager.class);

    public void uploadBackup(InputStream backupStream, DatabaseSettings databaseSettings,
                             List<StorageSettings> storageSettingsList, String databaseName,
                             boolean compress, int maxChunkSize) {
        SimpleDateFormat date = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss");
        if (!compress) {
            List<TextStorage> storageList = new ArrayList<>();
            String backupName = String.format("backup_%s_%s", databaseName, date.format(new Date()));
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

            for (StorageSettings storageSettings : storageSettingsList) {
                BackupProperties backupProperties = new BackupProperties(backupName, false,
                        storageSettings.getId(), databaseSettings.getId());
                backupPropertiesManager.save(backupProperties);
            }

            logger.info("Backup successfully saved onto multiple storages. Storages amount: {}", storageSettingsList.size());
            for (BackupProperties backupProperties : backupPropertiesManager.getAll()) {
                logger.info("Current backup properties: {}", backupProperties);
            }
        }
    }

    public void uploadBackup(InputStream backupStream, StorageSettings storageSettings, String databaseName,
                             boolean compress, int maxChunkSize) {

    }

    public void downloadBackup() {

    }
}
