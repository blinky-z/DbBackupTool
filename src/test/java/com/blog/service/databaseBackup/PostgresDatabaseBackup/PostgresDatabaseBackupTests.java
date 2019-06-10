package com.blog.service.databaseBackup.PostgresDatabaseBackup;

import com.blog.ApplicationTests;
import com.blog.entities.backup.BackupProperties;
import com.blog.entities.database.DatabaseSettings;
import com.blog.entities.storage.StorageSettings;
import com.blog.entities.storage.StorageType;
import com.blog.manager.*;
import com.blog.service.processor.ProcessorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static com.blog.TestUtils.clearDatabase;
import static com.blog.TestUtils.equalToMasterDatabase;
import static org.hamcrest.MatcherAssert.assertThat;

class PostgresDatabaseBackupTests extends ApplicationTests {
    private static final List<String> tableNames = new ArrayList<>(Arrays.asList("comments"));
    private static final Integer testTaskID = 0;

    @Autowired
    private JdbcTemplate jdbcPostgresMasterTemplate;
    @Autowired
    private JdbcTemplate jdbcPostgresSlaveTemplate;
    @Autowired
    private DatabaseSettings masterPostgresDatabaseSettings;
    @Autowired
    private DatabaseSettings slavePostgresDatabaseSettings;
    @Autowired
    private Map<StorageType, String> storageSettingsNameMap;
    @Autowired
    private DatabaseBackupManager databaseBackupManager;
    @Autowired
    private BackupProcessorManager backupProcessorManager;
    @Autowired
    private BackupLoadManager backupLoadManager;
    @Autowired
    private BackupPropertiesManager backupPropertiesManager;
    @Autowired
    private StorageSettingsManager storageSettingsManager;
    @Autowired
    private DatabaseSettingsManager databaseSettingsManager;
    @Autowired
    private List<StorageSettings> allStorageSettings;
    @Autowired
    private List<DatabaseSettings> allDatabaseSettings;

    @BeforeEach
    void init() {
        clearDatabase(jdbcPostgresMasterTemplate);
        clearDatabase(jdbcPostgresSlaveTemplate);
        storageSettingsManager.saveAll(allStorageSettings);
        databaseSettingsManager.saveAll(allDatabaseSettings);
        addTables(jdbcPostgresMasterTemplate);
    }

    void addTables(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("CREATE TABLE comments" +
                "(" +
                "ID        SERIAL PRIMARY KEY," +
                "AUTHOR    CHARACTER VARYING(36)   not null," +
                "DATE      TIMESTAMPTZ DEFAULT NOW()," +
                "CONTENT   CHARACTER VARYING(2048) not null" +
                ")");

        final long rowsToInsert = 10000L;
        jdbcTemplate.update("insert into comments (author, content)" +
                " select " +
                "    left(md5(i::text), 36)," +
                "    left(md5(random()::text), 2048) " +
                "from generate_series(0, ?) s(i)", rowsToInsert);
    }

    @Test
    void whenCreatePostgresBackupAndUploadToLocalFileSystemAndRestoreIntoSeparateDatabase_databasesAreEqual() throws IOException {
        String storageSettingsName = storageSettingsNameMap.get(StorageType.LOCAL_FILE_SYSTEM);

        try (
                InputStream backupStream = databaseBackupManager.createBackup(masterPostgresDatabaseSettings, testTaskID)
        ) {
            BackupProperties backupProperties = backupPropertiesManager.initNewBackupProperties(
                    storageSettingsName, null, masterPostgresDatabaseSettings.getName());
            backupLoadManager.uploadBackup(backupStream, backupProperties, testTaskID);
            try (
                    InputStream downloadedBackup = backupLoadManager.downloadBackup(backupProperties.getBackupName(),
                            storageSettingsName, testTaskID)
            ) {
                databaseBackupManager.restoreBackup(Objects.requireNonNull(downloadedBackup), slavePostgresDatabaseSettings, testTaskID);
            }
        }

        assertThat(jdbcPostgresMasterTemplate, equalToMasterDatabase(jdbcPostgresSlaveTemplate, tableNames));
    }

    @Test
    void whenCreatePostgresBackupAndUploadToDropboxAndRestoreIntoSeparateDatabase_databasesAreEqual() throws IOException {
        String storageSettingsName = storageSettingsNameMap.get(StorageType.DROPBOX);

        try (
                InputStream backupStream = databaseBackupManager.createBackup(masterPostgresDatabaseSettings, testTaskID)
        ) {
            BackupProperties backupProperties = backupPropertiesManager.initNewBackupProperties(
                    storageSettingsName, null, masterPostgresDatabaseSettings.getName());
            backupLoadManager.uploadBackup(backupStream, backupProperties, testTaskID);
            try (
                    InputStream downloadedBackup = backupLoadManager.downloadBackup(backupProperties.getBackupName(), storageSettingsName,
                            testTaskID)
            ) {
                databaseBackupManager.restoreBackup(Objects.requireNonNull(downloadedBackup), slavePostgresDatabaseSettings, testTaskID);
            }
        }

        assertThat(jdbcPostgresMasterTemplate, equalToMasterDatabase(jdbcPostgresSlaveTemplate, tableNames));
    }

    @Test
    void whenCreatePostgresBackupAndCompressAndUploadToLocalFileSystemAndDecompressAndRestoreIntoSeparateDatabase_databasesAreEqual()
            throws IOException {
        List<ProcessorType> processors = new ArrayList<>();
        processors.add(ProcessorType.COMPRESSOR);
        String storageSettingsName = storageSettingsNameMap.get(StorageType.LOCAL_FILE_SYSTEM);

        try (
                InputStream backupStream = databaseBackupManager.createBackup(masterPostgresDatabaseSettings, testTaskID);
                InputStream compressedBackup = backupProcessorManager.process(backupStream, processors)
        ) {
            BackupProperties backupProperties = backupPropertiesManager.initNewBackupProperties(
                    storageSettingsName, processors, masterPostgresDatabaseSettings.getName());
            backupLoadManager.uploadBackup(compressedBackup, backupProperties, testTaskID);
            try (
                    InputStream downloadedBackup = backupLoadManager.downloadBackup(backupProperties.getBackupName(), storageSettingsName, testTaskID);
                    InputStream decompressedBackup = backupProcessorManager.deprocess(Objects.requireNonNull(downloadedBackup), processors)
            ) {
                databaseBackupManager.restoreBackup(decompressedBackup, slavePostgresDatabaseSettings, testTaskID);
            }
        }

        assertThat(jdbcPostgresMasterTemplate, equalToMasterDatabase(jdbcPostgresSlaveTemplate, tableNames));
    }

    @Test
    void whenCreatePostgresBackupAndCompressAndUploadToDropboxAndDecompressAndRestoreIntoSeparateDatabase_databasesAreEqual()
            throws IOException {
        List<ProcessorType> processors = new ArrayList<>();
        processors.add(ProcessorType.COMPRESSOR);
        String storageSettingsName = storageSettingsNameMap.get(StorageType.DROPBOX);

        try (
                InputStream backupStream = databaseBackupManager.createBackup(masterPostgresDatabaseSettings, testTaskID);
                InputStream compressedBackup = backupProcessorManager.process(backupStream, processors)
        ) {
            BackupProperties backupProperties = backupPropertiesManager.initNewBackupProperties(
                    storageSettingsName, processors, masterPostgresDatabaseSettings.getName());
            backupLoadManager.uploadBackup(compressedBackup, backupProperties, testTaskID);

            try (
                    InputStream downloadedBackup = backupLoadManager.downloadBackup(backupProperties.getBackupName(), storageSettingsName, testTaskID);
                    InputStream decompressedBackup = backupProcessorManager.deprocess(Objects.requireNonNull(downloadedBackup), processors)
            ) {
                databaseBackupManager.restoreBackup(decompressedBackup, slavePostgresDatabaseSettings, testTaskID);
            }
        }

        assertThat(jdbcPostgresMasterTemplate, equalToMasterDatabase(jdbcPostgresSlaveTemplate, tableNames));
    }
}
