package com.example.demo;

import com.example.demo.DbDumpHandler.PostgresDumpHandler;
import com.example.demo.StorageHandler.FileSystemTextStorageHandler;
import com.example.demo.settings.DatabaseSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import static java.lang.Math.min;

@Service
public class DbBackup {
    @Autowired
    private PostgresDumpHandler postgresDumpHandler;

    @Autowired
    private FileSystemTextStorageHandler fileSystemTextStorageHandler;

    private static final Logger logger = LoggerFactory.getLogger(DbBackup.class);

    @Autowired
    private DatabaseSettings dbSettings;

    private long maxDefaultChunkSize = 1024L * 1024 * 2;

    public void backup(boolean compressData, long maxChunkSizeInBytes) {
        maxChunkSizeInBytes = min(maxChunkSizeInBytes, maxDefaultChunkSize);
        try {
            try (
                    BufferedReader dumpStreamReader = new BufferedReader(new InputStreamReader(postgresDumpHandler.createDbDump()));
            ) {
                StringBuilder currentChunk = new StringBuilder();
                int currentChunkSize = 0;
                String currentLine;
                while ((currentLine = dumpStreamReader.readLine()) != null) {
                    currentChunk.append(currentLine + "\n");
                    currentChunkSize += currentLine.getBytes().length;
                    if (currentChunkSize >= maxChunkSizeInBytes) {
                        fileSystemTextStorageHandler.saveBackup(currentChunk.toString());
                        currentChunkSize = 0;
                    }
                }
                if (currentChunkSize != 0) {
                    fileSystemTextStorageHandler.saveBackup(currentChunk.toString());
                }
            }

            logger.info("Backup successfully created. Database name: {}", dbSettings.getDatabaseName());
        } catch (IOException ex) {
            throw new RuntimeException("Error creating backup. Database name: " + dbSettings.getDatabaseName(), ex);
        }
    }
}