package com.example.demo.service.storage;

import com.example.demo.entities.storage.LocalFileSystemSettings;
import com.example.demo.entities.storage.StorageSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class FileSystemStorage implements Storage {
    private StorageSettings storageSettings;

    private LocalFileSystemSettings localFileSystemSettings;

    private static final Logger logger = LoggerFactory.getLogger(FileSystemStorage.class);

    private String backupName;

    private long currentBackupPart;

    private String backupFolderPath;

    private static final String FILE_EXTENSION = ".dat";

    private static final String FILENAME_TEMPLATE = "%s_part%d";

    private String getCurrentFilePartAsAbsolutePath() {
        String filename = String.format(FILENAME_TEMPLATE, backupName, currentBackupPart);
        return backupFolderPath + File.separator + filename + FILE_EXTENSION;
    }

    private File createNewFile() {
        File backupFolder = new File(backupFolderPath);
        if (!backupFolder.exists()) {
            if (!backupFolder.mkdir()) {
                throw new RuntimeException("Error creating backup folder");
            }
        }
        File currentFile = new File(getCurrentFilePartAsAbsolutePath());
        return currentFile;
    }

    public FileSystemStorage(StorageSettings storageSettings, String backupName) {
        this.storageSettings = storageSettings;
        this.localFileSystemSettings = storageSettings.getLocalFileSystemSettings().orElseThrow(() -> new RuntimeException(
                "Can't construct File System StorageType: Missing Settings"));
        this.backupName = backupName;
        String backupPath = localFileSystemSettings.getBackupPath().replace("/", File.separator);
        this.backupFolderPath = backupPath + File.separator + backupName;
    }

    /**
     * Saves backup on the File System.
     *
     * @param in the input stream to read backup from
     */
    @Override
    public void uploadBackup(InputStream in) {
        logger.info("Uploading backup chunk to the Local File System into folder {}", backupFolderPath);

        try (
                BufferedInputStream bufferedInputStream = new BufferedInputStream(in)
        ) {
            long maxChunkSize = 64L * 1024;
            long currentChunkSize;
            int bytesRead = 0;

            while (bytesRead != -1) {
                File currentFile = createNewFile();
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
                            currentBackupPart++;
                            break;
                        }
                    }
                    bufferedOutputStream.flush();
                }
            }
        } catch (IOException ex) {
            throw new RuntimeException("Error occurred while uploading backup chunk to the Local File System", ex);
        }

        logger.info("Backup chunk successfully saved on the Local File System. Backup folder: {}", backupFolderPath);
    }

    /**
     * Downloads backup from the file system.
     *
     * @return input stream, from which backup can be read
     */
    @Override
    public InputStream downloadBackup() {
        logger.info("Downloading backup from the Local File System. Backup folder: {}", backupFolderPath);
        try {
            List<InputStream> backupFileStreamList = new ArrayList<>();
            long filesCount = Objects.requireNonNull(new File(backupFolderPath).list(),
                    "Can't download backup from the Local File System: Missing backup folder").length;

            logger.info("Total files in backup folder on Local File System: {}. Backup folder: {}", filesCount, backupFolderPath);

            for (currentBackupPart = 0; currentBackupPart < filesCount; currentBackupPart++) {
                File backupFile = new File(getCurrentFilePartAsAbsolutePath());
                logger.info("Downloading file [{}]: '{}'", currentBackupPart, backupFile.getName());
                backupFileStreamList.add(new FileInputStream(backupFile));
            }

            logger.info("Downloading backup from the Local File System completed. Backup folder {}", backupFolderPath);

            return new SequenceInputStream(Collections.enumeration(backupFileStreamList));
        } catch (IOException ex) {
            throw new RuntimeException("Error occurred while downloading backup from Local File System", ex);
        }
    }
}
