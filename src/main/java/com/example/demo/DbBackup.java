package com.example.demo;

import models.Env;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import settings.DatabaseSettings;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@Service
public class DbBackup {
    private JdbcTemplate jdbcTemplate;

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
        Env env = new Env();
        env.jdbcTemplate = jdbcTemplate;
        env.logger = logger;
        DatabaseSettings dbSettings = new DatabaseSettings();
        dbSettings.connectionUrl = connectionUrl;
        dbSettings.databaseName = databaseName;
        dbSettings.databasePassword = databasePassword;
        dbSettings.databaseUser = databaseUser;
        env.dbSettings = dbSettings;

        try {
            PostgresDumpHandler dumpCreator = new PostgresDumpHandler(env);
            dumpCreator.createDbDump();

            BackupWriter backupWriter = new FileSystemBackupWriter(databaseName, maxChunkSizeInBytes, compressData);

            try (
                    BufferedReader dumpStreamReader = new BufferedReader(new InputStreamReader(dumpCreator.getDataStream()))
            ) {
                String currentLine;
                while ((currentLine = dumpStreamReader.readLine()) != null) {
                    backupWriter.write(currentLine);
                }
                backupWriter.close();
            }

            logger.info("Backup successfully created. Database name: {}", databaseName);
        } catch (IOException ex) {
            throw new RuntimeException("Error creating backup. Database name: " + databaseName, ex);
        }
    }


}