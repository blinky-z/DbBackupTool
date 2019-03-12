package com.example.demo.StorageHandler;

import com.example.demo.DbBackup;
import com.example.demo.settings.DatabaseSettings;
import com.example.demo.settings.UserSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class FileSystemTextStorageHandler implements TextStorageHandler {
    @Autowired
    private UserSettings userSettings;

    @Autowired
    private DatabaseSettings databaseSettings;

    private static final Logger logger = LoggerFactory.getLogger(DbBackup.class);

    private BufferedWriter fileWriter;

    private List<File> createdBackupFiles;

    public FileSystemTextStorageHandler() {
        createdBackupFiles = new ArrayList<>();
    }

    private void createNewFile() throws IOException {
        SimpleDateFormat date = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss-SS");
        String dateAsString = date.format(new Date());
        File currentFile = new File(userSettings.getBackupDir() + File.separator + "backup_" +
                databaseSettings.getDatabaseName() + "_" + dateAsString + ".data");
        logger.info("New created file: {}", currentFile.getAbsolutePath());
        createdBackupFiles.add(currentFile);
        fileWriter = new BufferedWriter(new FileWriter(currentFile));
    }

    @Override
    public void saveBackup(String data) {
        try {
            createNewFile();
            fileWriter.write(data);
            fileWriter.close();
        } catch (IOException ex) {
            throw new RuntimeException("Error occurred while writing data to file", ex);
        }
    }

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
