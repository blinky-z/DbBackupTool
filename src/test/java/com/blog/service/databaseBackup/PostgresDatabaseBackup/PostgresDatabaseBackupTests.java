package com.blog.service.databaseBackup.PostgresDatabaseBackup;

import com.blog.ApplicationTests;
import com.blog.TestUtils;
import com.blog.entities.backup.BackupProperties;
import com.blog.entities.database.DatabaseSettings;
import com.blog.entities.storage.StorageSettings;
import com.blog.entities.storage.StorageType;
import com.blog.manager.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

class PostgresDatabaseBackupTests extends ApplicationTests {
    private static final List<String> tableNames = new ArrayList<>(Arrays.asList("comments"));
    private static final Integer testTaskID = 0;
    private TestUtils testUtils;
    private JdbcTemplate jdbcPostgresMasterTemplate;
    private JdbcTemplate jdbcPostgresSlaveTemplate;
    private DatabaseSettings masterPostgresDatabaseSettings;
    private DatabaseSettings slavePostgresDatabaseSettings;
    private HashMap<StorageType, String> storageSettingsNameMap;
    private DatabaseBackupManager databaseBackupManager;
    private BackupProcessorManager backupProcessorManager;
    private BackupLoadManager backupLoadManager;
    private BackupPropertiesManager backupPropertiesManager;
    private StorageSettingsManager storageSettingsManager;
    private DatabaseSettingsManager databaseSettingsManager;
    @Autowired
    private List<StorageSettings> allStorageSettings;
    @Autowired
    private List<DatabaseSettings> allDatabaseSettings;

    @Autowired
    void setStorageSettingsManager(StorageSettingsManager storageSettingsManager) {
        this.storageSettingsManager = storageSettingsManager;
    }

    @Autowired
    void setDatabaseSettingsManager(DatabaseSettingsManager databaseSettingsManager) {
        this.databaseSettingsManager = databaseSettingsManager;
    }

    @Autowired
    void setTestUtils(TestUtils testUtils) {
        this.testUtils = testUtils;
    }

    @Autowired
    void setJdbcPostgresMasterTemplate(JdbcTemplate jdbcPostgresMasterTemplate) {
        this.jdbcPostgresMasterTemplate = jdbcPostgresMasterTemplate;
    }

    @Autowired
    void setJdbcPostgresSlaveTemplate(JdbcTemplate jdbcPostgresSlaveTemplate) {
        this.jdbcPostgresSlaveTemplate = jdbcPostgresSlaveTemplate;
    }

    @Autowired
    void setMasterPostgresDatabaseSettings(DatabaseSettings masterPostgresDatabaseSettings) {
        this.masterPostgresDatabaseSettings = masterPostgresDatabaseSettings;
    }

    @Autowired
    void setSlavePostgresDatabaseSettings(DatabaseSettings slavePostgresDatabaseSettings) {
        this.slavePostgresDatabaseSettings = slavePostgresDatabaseSettings;
    }

    @Autowired
    public void setBackupPropertiesManager(BackupPropertiesManager backupPropertiesManager) {
        this.backupPropertiesManager = backupPropertiesManager;
    }

    @Autowired
    public void setStorageSettingsNameMap(HashMap<StorageType, String> storageSettingsNameMap) {
        this.storageSettingsNameMap = storageSettingsNameMap;
    }

    @Autowired
    void setDatabaseBackupManager(DatabaseBackupManager databaseBackupManager) {
        this.databaseBackupManager = databaseBackupManager;
    }

    @Autowired
    void setBackupProcessorManager(BackupProcessorManager backupProcessorManager) {
        this.backupProcessorManager = backupProcessorManager;
    }

    @Autowired
    void setBackupLoadManager(BackupLoadManager backupLoadManager) {
        this.backupLoadManager = backupLoadManager;
    }

    @BeforeEach
    void init() {
        testUtils.clearDatabase(jdbcPostgresMasterTemplate);
        testUtils.clearDatabase(jdbcPostgresSlaveTemplate);
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
    void whenCreatePostgresBackupAndUploadToLocalFileSystemAndRestore_databasesIsEqual() throws IOException {
        List<String> processors = new ArrayList<>();
        String storageSettingsName = storageSettingsNameMap.get(StorageType.LOCAL_FILE_SYSTEM);

        try (
                InputStream backupStream = databaseBackupManager.createBackup(masterPostgresDatabaseSettings, testTaskID)
        ) {
            BackupProperties backupProperties = backupPropertiesManager.initNewBackupProperties(
                    Collections.singletonList(storageSettingsName), processors, masterPostgresDatabaseSettings.getName());
            backupLoadManager.uploadBackup(backupStream, backupProperties, testTaskID);
            try (
                    InputStream downloadedBackup = backupLoadManager.downloadBackup(backupProperties.getBackupName(),
                            storageSettingsName, testTaskID)
            ) {
                databaseBackupManager.restoreBackup(Objects.requireNonNull(downloadedBackup), slavePostgresDatabaseSettings, testTaskID);
            }
        }

        testUtils.compareLargeTables(tableNames, jdbcPostgresMasterTemplate, jdbcPostgresSlaveTemplate);
    }

    @Test
    void whenCreatePostgresBackupAndUploadToDropboxAndRestore_databasesIsEqual() throws IOException {
        List<String> processors = new ArrayList<>();
        String storageSettingsName = storageSettingsNameMap.get(StorageType.DROPBOX);

        try (
                InputStream backupStream = databaseBackupManager.createBackup(masterPostgresDatabaseSettings, testTaskID)
        ) {
            BackupProperties backupProperties = backupPropertiesManager.initNewBackupProperties(
                    Collections.singletonList(storageSettingsName), processors, masterPostgresDatabaseSettings.getName());
            backupLoadManager.uploadBackup(backupStream, backupProperties, testTaskID);
            try (
                    InputStream downloadedBackup = backupLoadManager.downloadBackup(backupProperties.getBackupName(), storageSettingsName,
                            testTaskID)
            ) {
                databaseBackupManager.restoreBackup(Objects.requireNonNull(downloadedBackup), slavePostgresDatabaseSettings, testTaskID);
            }
        }

        testUtils.compareLargeTables(tableNames, jdbcPostgresMasterTemplate, jdbcPostgresSlaveTemplate);
    }

    @Test
    void whenCreatePostgresBackupAndCompressAndUploadToLocalFileSystemAndDecompressAndRestore_databasesIsEqual() throws IOException {
        List<String> processors = new ArrayList<>();
        processors.add("Compressor");
        String storageSettingsName = storageSettingsNameMap.get(StorageType.LOCAL_FILE_SYSTEM);

        try (
                InputStream backupStream = databaseBackupManager.createBackup(masterPostgresDatabaseSettings, testTaskID);
                InputStream compressedBackup = backupProcessorManager.process(backupStream, processors)
        ) {
            BackupProperties backupProperties = backupPropertiesManager.initNewBackupProperties(
                    Collections.singletonList(storageSettingsName), processors, masterPostgresDatabaseSettings.getName());
            backupLoadManager.uploadBackup(compressedBackup, backupProperties, testTaskID);
            try (
                    InputStream downloadedBackup = backupLoadManager.downloadBackup(backupProperties.getBackupName(), storageSettingsName, testTaskID);
                    InputStream decompressedBackup = backupProcessorManager.deprocess(Objects.requireNonNull(downloadedBackup), processors)
            ) {
                databaseBackupManager.restoreBackup(decompressedBackup, slavePostgresDatabaseSettings, testTaskID);
            }
        }

        testUtils.compareLargeTables(tableNames, jdbcPostgresMasterTemplate, jdbcPostgresSlaveTemplate);
    }

    @Test
    void whenCreatePostgresBackupAndCompressAndUploadToDropboxAndDecompressAndRestore_databasesIsEqual() throws IOException {
        List<String> processors = new ArrayList<>();
        processors.add("Compressor");
        String storageSettingsName = storageSettingsNameMap.get(StorageType.DROPBOX);

        try (
                InputStream backupStream = databaseBackupManager.createBackup(masterPostgresDatabaseSettings, testTaskID);
                InputStream compressedBackup = backupProcessorManager.process(backupStream, processors)
        ) {
            BackupProperties backupProperties = backupPropertiesManager.initNewBackupProperties(
                    Collections.singletonList(storageSettingsName), processors, masterPostgresDatabaseSettings.getName());
            backupLoadManager.uploadBackup(compressedBackup, backupProperties, testTaskID);

            try (
                    InputStream downloadedBackup = backupLoadManager.downloadBackup(backupProperties.getBackupName(), storageSettingsName, testTaskID);
                    InputStream decompressedBackup = backupProcessorManager.deprocess(Objects.requireNonNull(downloadedBackup), processors)
            ) {
                databaseBackupManager.restoreBackup(decompressedBackup, slavePostgresDatabaseSettings, testTaskID);
            }
        }

        testUtils.compareLargeTables(tableNames, jdbcPostgresMasterTemplate, jdbcPostgresSlaveTemplate);
    }
}
