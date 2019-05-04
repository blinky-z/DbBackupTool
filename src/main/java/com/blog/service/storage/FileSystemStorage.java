package com.blog.service.storage;

import com.blog.entities.storage.LocalFileSystemSettings;
import com.blog.entities.storage.StorageSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Service
public class FileSystemStorage implements Storage {
    private static final Logger logger = LoggerFactory.getLogger(FileSystemStorage.class);

    private String getCurrentFilePartAsAbsolutePath(String backupFolderPath, String backupName, long currentBackupPart) {
        String filename = String.format(FILENAME_TEMPLATE, backupName, currentBackupPart);
        return backupFolderPath + File.separator + filename + FILE_EXTENSION;
    }

    /**
     * Uploads backup to Local File System
     */
    @Override
    public void uploadBackup(InputStream in, StorageSettings storageSettings, String backupName) {
        LocalFileSystemSettings localFileSystemSettings = storageSettings.getLocalFileSystemSettings().orElseThrow(() ->
                new RuntimeException("Can't upload backup to Local File System storage: Missing Storage Settings"));
        String backupFolderPath = localFileSystemSettings.getBackupPath().replace("/", File.separator);
        backupFolderPath = backupFolderPath + File.separator + backupName;

        logger.info("Uploading backup to the Local File System. Backup folder: {}", backupFolderPath);

        try (
                BufferedInputStream bufferedInputStream = new BufferedInputStream(in)
        ) {
            long maxChunkSize = 64L * 1024;
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
            throw new RuntimeException("Error occurred while uploading backup chunk to the Local File System", ex);
        }

        logger.info("Backup successfully saved on the Local File System. Backup folder: {}", backupFolderPath);
    }

    /**
     * Downloads backup from Local File System
     */
    @Override
    public InputStream downloadBackup(StorageSettings storageSettings, String backupName) {
        LocalFileSystemSettings localFileSystemSettings = storageSettings.getLocalFileSystemSettings().orElseThrow(() ->
                new RuntimeException("Can't download backup from Local File System storage: Missing Storage Settings"));
        String backupFolderPath = localFileSystemSettings.getBackupPath().replace("/", File.separator);
        backupFolderPath = backupFolderPath + File.separator + backupName;

        logger.info("Downloading backup from the Local File System. Backup folder: {}", backupFolderPath);
        try {
            List<InputStream> backupFileStreamList = new ArrayList<>();
            long filesCount = Objects.requireNonNull(new File(backupFolderPath).list(),
                    "Can't download backup from the Local File System: Missing backup folder").length;

            logger.info("Total files in backup folder on Local File System: {}. Backup folder: {}", filesCount, backupFolderPath);

            for (long currentBackupPart = 0; currentBackupPart < filesCount; currentBackupPart++) {
                File backupFile = new File(getCurrentFilePartAsAbsolutePath(backupFolderPath, backupName, currentBackupPart));
                logger.info("Downloading file [{}/{}]: '{}'", currentBackupPart + 1, filesCount, backupFile.getName());
                backupFileStreamList.add(new FileInputStream(backupFile));
            }

            logger.info("Downloading backup from the Local File System completed. Backup folder {}", backupFolderPath);

            return new SequenceInputStream(Collections.enumeration(backupFileStreamList));
        } catch (IOException ex) {
            throw new RuntimeException("Error occurred while downloading backup from Local File System", ex);
        }
    }
}
