package com.blog.service.storage;

import com.blog.entities.storage.LocalFileSystemSettings;
import com.blog.entities.storage.StorageSettings;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Implementation of {@link Storage} interface for Local File System.
 */
@Service
public class FileSystemStorage implements Storage {
    private static final Logger logger = LoggerFactory.getLogger(FileSystemStorage.class);

    private String getCurrentFilePartAsAbsolutePath(String backupFolderPath, String backupName, long currentBackupPart) {
        String filename = String.format(StorageConstants.DEFAULT_FILENAME_TEMPLATE, backupName, currentBackupPart);
        return backupFolderPath + File.separator + filename + StorageConstants.DEFAULT_FILE_EXTENSION;
    }

    /**
     * Uploads backup to Local File System
     */
    @Override
    public void uploadBackup(InputStream in, StorageSettings storageSettings, String backupName, Integer id) {
        LocalFileSystemSettings localFileSystemSettings = storageSettings.getLocalFileSystemSettings().orElseThrow(() ->
                new RuntimeException("Can't upload backup to Local File System storage: Missing Storage Settings"));
        String backupFolderPath = localFileSystemSettings.getBackupPath().replace("/", File.separator);
        backupFolderPath = backupFolderPath + File.separator + backupName;

        try (
                BufferedInputStream bufferedInputStream = new BufferedInputStream(in)
        ) {
            long maxChunkSize = 1024L * 1024 * 192;
            long currentBackupPart = 0;
            long currentChunkSize;
            int bytesRead = 0;

            File backupFolder = new File(backupFolderPath);
            if (!backupFolder.mkdir()) {
                throw new RuntimeException(String.format(
                        "Can't upload backup to Local File System storage: error creating backup folder to save backup to. " +
                                "Backup folder: %s", backupFolderPath));
            }

            while (bytesRead != -1) {
                File currentFile = new File(getCurrentFilePartAsAbsolutePath(backupFolderPath, backupName, currentBackupPart));
                currentChunkSize = 0;

                try (
                        FileOutputStream fileOutputStream = new FileOutputStream(currentFile);
                        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream)
                ) {
                    byte[] buffer = new byte[64 * 1024];
                    while ((bytesRead = bufferedInputStream.read(buffer)) != -1) {
                        currentChunkSize += bytesRead;
                        bufferedOutputStream.write(buffer, 0, bytesRead);

                        if (currentChunkSize >= maxChunkSize) {
                            bufferedOutputStream.flush();
                            currentBackupPart++;
                            break;
                        }
                    }
                }
            }
        } catch (IOException ex) {
            throw new RuntimeException("Error occurred while uploading backup to the Local File System", ex);
        }
    }

    /**
     * Downloads backup from Local File System
     */
    @Nullable
    @Override
    public InputStream downloadBackup(StorageSettings storageSettings, String backupName, Integer id) {
        LocalFileSystemSettings localFileSystemSettings = storageSettings.getLocalFileSystemSettings().orElseThrow(() ->
                new RuntimeException("Can't download backup from Local File System storage: Missing Storage Settings"));
        String backupFolderPath = localFileSystemSettings.getBackupPath().replace("/", File.separator);
        backupFolderPath = backupFolderPath + File.separator + backupName;

        try {
            List<InputStream> backupFileStreamList = new ArrayList<>();

            File backupFolder = new File(backupFolderPath);
            long filesCount = Objects.requireNonNull(backupFolder.list(),
                    String.format("Can't download backup: invalid backup folder path: %s", backupFolderPath)).length;

            logger.info("Total files in backup folder on Local File System: {}. Backup folder: {}", filesCount, backupFolderPath);

            for (long currentBackupPart = 0; currentBackupPart < filesCount; currentBackupPart++) {
                File backupFile = new File(getCurrentFilePartAsAbsolutePath(backupFolderPath, backupName, currentBackupPart));
                backupFileStreamList.add(new FileInputStream(backupFile));
            }

            return new SequenceInputStream(Collections.enumeration(backupFileStreamList));
        } catch (IOException ex) {
            throw new RuntimeException("Error occurred while downloading backup from Local File System", ex);
        }
    }

    @Override
    public void deleteBackup(StorageSettings storageSettings, String backupName, Integer id) {
        LocalFileSystemSettings localFileSystemSettings = storageSettings.getLocalFileSystemSettings().orElseThrow(() ->
                new RuntimeException("Can't delete backup from Local File System storage: Missing Storage Settings"));
        String backupFolderPath = localFileSystemSettings.getBackupPath().replace("/", File.separator);
        backupFolderPath = backupFolderPath + File.separator + backupName;

        File backupFolder = new File(backupFolderPath);
        long filesCount = Objects.requireNonNull(backupFolder.list(),
                String.format("Can't delete backup: invalid path or non-existing backup folder. Path: %s", backupFolderPath)).length;

        logger.info("Total files in backup folder on Local File System: {}. Backup folder: {}", filesCount, backupFolderPath);

        boolean deleted;
        for (long currentBackupPart = 0; currentBackupPart < filesCount; currentBackupPart++) {
            File backupFile = new File(getCurrentFilePartAsAbsolutePath(backupFolderPath, backupName, currentBackupPart));
            deleted = backupFile.delete();
            if (!deleted) {
                throw new RuntimeException(String.format("Error deleting backup. Backup folder: %s. Can't delete file: %s",
                        backupFolderPath, backupFile.getName()));
            }
        }
        deleted = backupFolder.delete();
        if (!deleted) {
            throw new RuntimeException(String.format("Error deleting backup. Backup folder: %s. Can't delete folder",
                    backupFolderPath));
        }
    }
}
