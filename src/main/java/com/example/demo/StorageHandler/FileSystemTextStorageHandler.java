package com.example.demo.StorageHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import settings.DatabaseSettings;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class FileSystemTextStorageHandler implements TextStorageHandler {
    @Value("userConfig.backup-dir")
    private String backupDir;

    private DatabaseSettings dbSettings;

    private BufferedWriter fileWriter;

    private List<File> createdBackupFiles;

    @Autowired
    public FileSystemTextStorageHandler(DatabaseSettings dbSettings) {
        this.dbSettings = dbSettings;
        createdBackupFiles = new ArrayList<>();
    }

    private void createNewFile() throws IOException {
        SimpleDateFormat date = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss");
        String dateAsString = date.format(new Date());
        File currentFile = new File(backupDir + File.separator + "backup_" + dbSettings.getDatabaseName() + "_"
                + dateAsString + ".data");
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
            SimpleDateFormat date = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss");
            String dateAsString = date.format(new Date());

            File backupAsSingleFile = new File(System.getProperty("java.io.tmpdir") + File.separator + "backup_" +
                    dbSettings.getDatabaseName() + "_" + dateAsString + ".data");
            BufferedWriter backupWriter = new BufferedWriter(new FileWriter(backupAsSingleFile));
            for (File currentBackupFile : createdBackupFiles) {
                BufferedReader fileReader = new BufferedReader(new FileReader(currentBackupFile));
                String currentLine;
                while ((currentLine = fileReader.readLine()) != null) {
                    backupWriter.write(currentLine);
                }
                fileReader.close();
            }
            backupWriter.close();

            return new FileInputStream(backupAsSingleFile);
        } catch (IOException ex) {
            throw new RuntimeException("Error occurred while downloading backup", ex);
        }
    }
}
