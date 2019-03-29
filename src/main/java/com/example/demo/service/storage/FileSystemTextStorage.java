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

/**
 * This class is used to write and download plain text backup from local file system.
 */
public class FileSystemTextStorage implements TextStorage {
    private StorageSettings storageSettings;

    private LocalFileSystemSettings localFileSystemSettings;

    private static final Logger logger = LoggerFactory.getLogger(FileSystemTextStorage.class);

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
                throw new RuntimeException("Error creating backup folder on Local File System to save backup into");
            }
        }
        File currentFile = new File(getCurrentFilePartAsAbsolutePath());
        currentBackupPart++;
        return currentFile;
    }

    public FileSystemTextStorage(StorageSettings storageSettings, String backupName) {
        this.storageSettings = storageSettings;
        this.localFileSystemSettings = storageSettings.getLocalFileSystemSettings().orElseThrow(RuntimeException::new);
        this.backupName = backupName;
        this.backupFolderPath = localFileSystemSettings.getBackupPath() + File.separator + backupName;
    }

    /**
     * Saves backup chunk on the file system.
     * Each call creates new file.
     *
     * @param data backup chunk to save
     */
    @Override
    public void uploadBackup(String data) {
        logger.info("Uploading backup chunk to the Local File System into folder {}", backupFolderPath);
        File currentFile = createNewFile();
        try (
                FileWriter fileWriter = new FileWriter(currentFile);
                BufferedWriter bufferedWriter = new BufferedWriter(fileWriter)
        ) {
            bufferedWriter.write(data);
        } catch (IOException ex) {
            throw new RuntimeException("Error occurred while uploading backup chunk to the Local File System", ex);
        }

        logger.info("Backup chunk successfully saved on the Local File System. Created file: {}", currentFile.getName());
    }

    /**
     * Downloads backup from the local file system.
     *
     * @return input stream, from which backup can be read
     */
    @Override
    public InputStream downloadBackup() {
        logger.info("Downloading plain-text backup from the Local File System. Backup folder: {}", backupFolderPath);
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

            logger.info("Downloading of plain-text backup from the Local File System successfully completed. Backup folder {}",
                    backupFolderPath);

            return new SequenceInputStream(Collections.enumeration(backupFileStreamList));
        } catch (IOException ex) {
            throw new RuntimeException(
                    String.format("Error occurred while downloading plain-text backup from the Local File System. Backup folder: %s",
                            backupFolderPath), ex);
        }
    }
}
