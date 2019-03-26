package com.example.demo.service.storage;

import com.example.demo.entities.storage.LocalFileSystemSettings;
import com.example.demo.entities.storage.StorageSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

/**
 * This class is used to write and download plain text backup from local file system.
 */
public class FileSystemTextStorage implements TextStorage {
    private StorageSettings storageSettings;

    private LocalFileSystemSettings localFileSystemSettings;

    private static final Logger logger = LoggerFactory.getLogger(FileSystemTextStorage.class);

    private BufferedWriter fileWriter;

    private String backupName;

    private long currentBackupPart;

    private String backupFolderPath;

    private static String FILENAME_TEMPLATE = "%s" + File.separator + "%s_part%d.data";

    private String getFilename() {
        return String.format(FILENAME_TEMPLATE, backupFolderPath, backupName, currentBackupPart);
    }

    private void createNewFile() throws IOException {
        File backupFolder = new File(backupFolderPath);
        if (!backupFolder.exists()) {
            if (!backupFolder.mkdir()) {
                throw new RuntimeException("Can't upload backup to file system storage: error creating backup folder");
            }
        }
        File currentFile = new File(getFilename());
        logger.info("New created backup file: {}", currentFile.getAbsolutePath());
        fileWriter = new BufferedWriter(new FileWriter(currentFile));
        currentBackupPart++;
    }

    public FileSystemTextStorage(StorageSettings storageSettings, String backupName) {
        this.storageSettings = storageSettings;
        this.localFileSystemSettings = storageSettings.getLocalFileSystemSettings().orElseThrow(RuntimeException::new);
        this.backupName = backupName;
        this.backupFolderPath = localFileSystemSettings.getBackupPath() + File.separator + backupName;
    }

    /**
     * Saves backup chunk to the local file system.
     * Each call creates new file containing backup chunk.
     *
     * @param data backup chunk to be saved to the file system
     */
    @Override
    public void uploadBackup(String data) {
        try {
            createNewFile();
            fileWriter.write(data);
            fileWriter.close();
        } catch (IOException ex) {
            throw new RuntimeException("Error occurred while writing data to file", ex);
        }
    }

    /**
     * Downloads backup from the file system.
     *
     * @return input stream containing the whole backup.
     */
    @Override
    public InputStream downloadBackup() {
        try {
            List<InputStream> backupFilesStreams = new ArrayList<>();

            long filesCount = new File(backupFolderPath).list().length;
            for (currentBackupPart = 0; currentBackupPart < filesCount; currentBackupPart++) {
                File backupFile = new File(getFilename());
                backupFilesStreams.add(new FileInputStream(backupFile));
            }

            return new SequenceInputStream(Collections.enumeration(backupFilesStreams));
        } catch (IOException ex) {
            throw new RuntimeException("Error occurred while downloading backup", ex);
        }
    }
}
