package com.example.demo;

import com.example.demo.StorageHandler.FileSystemTextStorageHandler;
import com.example.demo.DbDumpHandler.PostgresDumpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@Service
public class DbBackup {
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PostgresDumpHandler postgresDumpHandler;

    @Autowired
    private FileSystemTextStorageHandler fileSystemTextStorageHandler;

    private static final Logger logger = LoggerFactory.getLogger(DbBackup.class);

    @Autowired
    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Value("${spring.datasource.url}")
    private String connectionUrl;
    @Value("${spring.datasource.username}")
    private String databaseUser;
    @Value("${spring.datasource.password}")
    private String databasePassword;
    @Value("${userConfig.database-name}")
    private String databaseName;

    public void backup(boolean compressData, long maxChunkSizeInBytes) {
        try {
            try (
                    BufferedReader dumpStreamReader = new BufferedReader(new InputStreamReader(postgresDumpHandler.createDbDump()));
            ) {
                StringBuilder currentChunk = new StringBuilder();
                int currentChunkSize = 0;
                String currentLine;
                while ((currentLine = dumpStreamReader.readLine()) != null) {
                    currentChunk.append(currentLine);
                    currentChunkSize += currentLine.getBytes().length;
                    if (currentChunkSize >= maxChunkSizeInBytes) {
                        fileSystemTextStorageHandler.saveBackup(currentChunk.toString());
                    }
                }
            }

            logger.info("Backup successfully created. Database name: {}", databaseName);
        } catch (IOException ex) {
            throw new RuntimeException("Error creating backup. Database name: " + databaseName, ex);
        }
    }


}