package com.example.demo.service.storage;

import com.example.demo.entities.storage.LocalFileSystemSettings;
import com.example.demo.entities.storage.StorageSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class FileSystemBinaryStorage implements BinaryStorage {
    private StorageSettings storageSettings;

    private LocalFileSystemSettings localFileSystemSettings;

    private static final Logger logger = LoggerFactory.getLogger(FileSystemBinaryStorage.class);

    private ZipOutputStream zipFileWriter;

    private String backupName;

    private long currentBackupPart;

    private String backupFolderPath;

    private static String FILENAME_TEMPLATE = "%s_part%d";

    private String getFilename() {
        return String.format(FILENAME_TEMPLATE, backupName, currentBackupPart);
    }

    private void createNewFile() throws IOException {
        File backupFolder = new File(backupFolderPath);
        if (!backupFolder.exists()) {
            if (!backupFolder.mkdir()) {
                throw new RuntimeException("Can't upload backup to file system storage: error creating backup folder");
            }
        }
        String filename = getFilename();
        File currentFile = new File(backupFolderPath + File.separator + filename + ".zip");
        logger.info("New created backup file: {}", currentFile.getAbsolutePath());
        zipFileWriter = new ZipOutputStream(new FileOutputStream(currentFile));
        ZipEntry zipEntry = new ZipEntry(filename);
        zipFileWriter.putNextEntry(zipEntry);
        currentBackupPart++;
    }

    public FileSystemBinaryStorage(StorageSettings storageSettings, String backupName) {
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
    public void uploadBackup(byte[] data) {
        try {
            createNewFile();
            zipFileWriter.write(data);
            zipFileWriter.closeEntry();
            zipFileWriter.close();
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
            List<InputStream> backupFileStreamList = new ArrayList<>();

            long filesCount = new File(backupFolderPath).list().length;
            for (currentBackupPart = 0; currentBackupPart < filesCount; currentBackupPart++) {
                File backupFile = new File(backupFolderPath + File.separator + getFilename() + ".zip");

                FileInputStream fileInputStream = new FileInputStream(backupFile);
                ZipInputStream zipInputStream = new ZipInputStream(fileInputStream);

                backupFileStreamList.add(zipInputStream);
            }

            return new SequenceInputStream(Collections.enumeration(backupFileStreamList));
        } catch (IOException ex) {
            throw new RuntimeException("Error occurred while downloading backup", ex);
        }
    }
}
