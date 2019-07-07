package com.blog.service.storage;

import com.blog.entities.storage.LocalFileSystemSettings;
import com.blog.entities.storage.StorageSettings;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Implementation of the {@link Storage} interface for Local File System.
 */
@Service
public class FileSystemStorage implements Storage {
    private static final Logger logger = LoggerFactory.getLogger(FileSystemStorage.class);

    private String getCurrentFilePartAsAbsolutePath(String backupFolderPath, String backupName, long currentBackupPart) {
        String filename = String.format(StorageConstants.DEFAULT_FILENAME_TEMPLATE, backupName, currentBackupPart);
        return backupFolderPath + File.separator + filename + StorageConstants.DEFAULT_FILE_EXTENSION;
    }

    private String getSystemDependentPath(String path) {
        return path.replace("/", File.separator);
    }

    private String getBackupFolderPathFromCurrentFolder(String path, String backupName) {
        return path + File.separator + backupName;
    }

    @Override
    public void uploadBackup(InputStream in, StorageSettings storageSettings, String backupName, Integer id) {
        LocalFileSystemSettings localFileSystemSettings = storageSettings.getLocalFileSystemSettings().orElseThrow(() ->
                new RuntimeException("Can't upload backup to Local File System storage: Missing Storage Settings"));
        String backupFolderPath = getBackupFolderPathFromCurrentFolder(getSystemDependentPath(localFileSystemSettings.getBackupPath()), backupName);

        try (
                BufferedInputStream bufferedInputStream = new BufferedInputStream(in)
        ) {
            long maxChunkSize = 1024L * 1024 * 192;
            long currentBackupPart = 0;
            long currentChunkSize;
            int bytesRead = 0;

            File backupFolder = new File(backupFolderPath);
            if (!backupFolder.mkdir()) {
                throw new RuntimeException(
                        "Can't upload backup to Local File System storage: error creating backup folder to save backup to. Backup folder: "
                                + backupFolderPath);
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
        } catch (InterruptedIOException ex) {
            logger.error("Backup uploading to Local File System was interrupted. Backup folder: {}", backupFolderPath);
        } catch (IOException ex) {
            throw new RuntimeException(
                    "Error occurred while uploading backup to the Local File System. Backup folder: " + backupFolderPath, ex);
        }
    }

    @Nullable
    @Override
    public InputStream downloadBackup(StorageSettings storageSettings, String backupName, Integer id) {
        LocalFileSystemSettings localFileSystemSettings = storageSettings.getLocalFileSystemSettings().orElseThrow(() ->
                new RuntimeException("Can't download backup from Local File System storage: Missing Storage Settings"));
        String backupFolderPath = getBackupFolderPathFromCurrentFolder(getSystemDependentPath(localFileSystemSettings.getBackupPath()), backupName);

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
        } catch (FileNotFoundException ex) {
            throw new RuntimeException("Error occurred while downloading backup from Local File System", ex);
        }
    }

    @Override
    public void deleteBackup(StorageSettings storageSettings, String backupName, Integer id) {
        LocalFileSystemSettings localFileSystemSettings = storageSettings.getLocalFileSystemSettings().orElseThrow(() ->
                new RuntimeException("Can't delete backup from Local File System storage: Missing Storage Settings"));
        String backupFolderPathAsString = getBackupFolderPathFromCurrentFolder(getSystemDependentPath(localFileSystemSettings.getBackupPath()), backupName);
        Path backupFolderPath = Path.of(backupFolderPathAsString);

        try {
            FileSystemUtils.deleteRecursively(backupFolderPath);
        } catch (IOException ex) {
            throw new RuntimeException("Error deleting backup. Backup folder: " + backupFolderPathAsString, ex);
        }
    }
}
