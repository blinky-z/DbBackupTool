package com.example.demo;

import com.example.demo.BackupManager.PostgresBackupManager;
import com.example.demo.storage.FileSystemTextStorage;
import com.example.demo.settings.DatabaseSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@Service
public class DbBackup {
    @Autowired
    private PostgresBackupManager postgresBackupManager;

    @Autowired
    private FileSystemTextStorage fileSystemTextStorage;

    private static final Logger logger = LoggerFactory.getLogger(DbBackup.class);

    @Autowired
    private DatabaseSettings databaseSettings;

    private long maxDefaultChunkSize = 1024L * 1024 * 100;

    public void backup(boolean compressData, long chunkSize) {
        if (chunkSize > maxDefaultChunkSize) {
            chunkSize = maxDefaultChunkSize;
        }
        try (
                BufferedReader backupReader = new BufferedReader(new InputStreamReader(postgresBackupManager.createDbDump()))
        ) {
            StringBuilder currentChunk = new StringBuilder();
            int currentChunkSize = 0;
            String currentLine;
            while ((currentLine = backupReader.readLine()) != null) {
                currentChunk.append(currentLine).append(System.lineSeparator());
                currentChunkSize += currentLine.getBytes().length;

                if (currentChunkSize >= chunkSize) {
                    fileSystemTextStorage.saveBackup(currentChunk.toString());
                    currentChunkSize = 0;
                }
            }
            if (currentChunkSize != 0) {
                fileSystemTextStorage.saveBackup(currentChunk.toString());
            }

            logger.info("Backup successfully created. Database: {}", databaseSettings.getDatabaseName());
        } catch (IOException ex) {
            throw new RuntimeException("Error creating backup. Database: " + databaseSettings.getDatabaseName(), ex);
        }
    }
}