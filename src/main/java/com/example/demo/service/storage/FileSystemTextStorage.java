package com.example.demo.service.storage;

import com.example.demo.entities.storage.LocalFileSystemSettings;
import com.example.demo.entities.storage.StorageSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * This class is used to write and download plain text backup from local file system.
 */
public class FileSystemTextStorage implements TextStorage {
    StorageSettings storageSettings;

    LocalFileSystemSettings localFileSystemSettings;

    private static final Logger logger = LoggerFactory.getLogger(FileSystemTextStorage.class);

    private BufferedWriter fileWriter;

    private String databaseName;

    private List<File> createdBackupFiles;

    private SimpleDateFormat date;

    public FileSystemTextStorage() {
        createdBackupFiles = new ArrayList<>();
    }

    private void createNewFile() throws IOException {
        String dateAsString = date.format(new Date());
        File currentFile = new File(localFileSystemSettings.getBackupPath() + File.separator + "backup_" +
                databaseName + "_" + dateAsString + ".data");
        logger.info("New created backup file: {}", currentFile.getAbsolutePath());
        createdBackupFiles.add(currentFile);
        fileWriter = new BufferedWriter(new FileWriter(currentFile));
    }

    public FileSystemTextStorage(StorageSettings storageSettings, String databaseName) {
        date = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss-SS");
        this.storageSettings = storageSettings;
        this.localFileSystemSettings = storageSettings.getLocalFileSystemSettings().orElseThrow(RuntimeException::new);
        this.databaseName = databaseName;
    }

    /**
     * Saves backup chunk to the local file system.
     * Each call creates new file containing backup chunk.
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
     * @return input stream containing the whole backup.
     */
    @Override
    public InputStream downloadBackup() {
        try {
            List<InputStream> backupFilesStreams = new ArrayList<>();
            for (File currentBackupFile : createdBackupFiles) {
                backupFilesStreams.add(new FileInputStream(currentBackupFile));
            }

            return new SequenceInputStream(Collections.enumeration(backupFilesStreams));
        } catch (IOException ex) {
            throw new RuntimeException("Error occurred while downloading backup", ex);
        }
    }
}
