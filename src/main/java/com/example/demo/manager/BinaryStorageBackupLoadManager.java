package com.example.demo.manager;

import com.example.demo.entities.backup.BackupProperties;
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

@Component
public class BinaryStorageBackupLoadManager {
    private BackupPropertiesManager backupPropertiesManager;
    private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss-SS");

    @Autowired
    public void setBackupPropertiesManager(BackupPropertiesManager backupPropertiesManager) {
        this.backupPropertiesManager = backupPropertiesManager;
    }

    private static final Logger logger = LoggerFactory.getLogger(DatabaseBackupManager.class);

    public List<BackupProperties> uploadBackup(InputStream backupStream, List<StorageSettings> storageSettingsList,
                                               String databaseName, int maxChunkSize) {
        List<BinaryStorage> storageList = new ArrayList<>();
        Date creationTime = new Date();
        String backupName = String.format("backup_%s_%s", databaseName, SIMPLE_DATE_FORMAT.format(creationTime));
        for (StorageSettings storageSettings : storageSettingsList) {
            switch (storageSettings.getType()) {
                case LOCAL_FILE_SYSTEM: {
                    storageList.add(new FileSystemBinaryStorage(storageSettings, backupName));
                    break;
                }
                case DROPBOX: {
                    storageList.add(new DropboxBinaryStorage(storageSettings, backupName));
                    break;
                }
                default: {
                    throw new RuntimeException("Can't upload backup: Unknown storage type");
                }
            }
        }

        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

            final byte[] buffer = new byte[8 * 1024];
            int bytesRead;
            int currentChunkSize = 0;
            while ((bytesRead = backupStream.read(buffer)) != -1) {
                currentChunkSize += bytesRead;
                byteArrayOutputStream.write(buffer, 0, bytesRead);
                byteArrayOutputStream.write(System.lineSeparator().getBytes());

                if (currentChunkSize >= maxChunkSize) {
                    for (BinaryStorage currentStorage : storageList) {
                        currentStorage.uploadBackup(byteArrayOutputStream.toByteArray());
                    }
                    byteArrayOutputStream.reset();
                    currentChunkSize = 0;
                }
            }
            if (currentChunkSize != 0) {
                for (BinaryStorage currentStorage : storageList) {
                    currentStorage.uploadBackup(byteArrayOutputStream.toByteArray());
                }
            }
        } catch (IOException ex) {
            throw new RuntimeException("Error occurred while reading backup", ex);
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
        String backupName = backupProperties.getBackupName();
        switch (storageSettings.getType()) {
            case LOCAL_FILE_SYSTEM: {
                FileSystemBinaryStorage fileSystemBinaryStorage = new FileSystemBinaryStorage(storageSettings, backupName);
                return fileSystemBinaryStorage.downloadBackup();
            }
            case DROPBOX: {
                DropboxBinaryStorage dropboxBinaryStorage = new DropboxBinaryStorage(storageSettings, backupName);
                return dropboxBinaryStorage.downloadBackup();
            }
            default: {
                throw new RuntimeException("Can't download backup: Unknown storage type");
            }
        }
    }
}
