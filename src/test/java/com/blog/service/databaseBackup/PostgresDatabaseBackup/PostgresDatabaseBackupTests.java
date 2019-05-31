package com.blog.service.databaseBackup.PostgresDatabaseBackup;

import com.blog.ApplicationTests;
import com.blog.TestUtils;
import com.blog.entities.backup.BackupProperties;
import com.blog.entities.database.DatabaseSettings;
import com.blog.entities.storage.StorageSettings;
import com.blog.manager.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class PostgresDatabaseBackupTests extends ApplicationTests {
    private static final List<String> tableNames = new ArrayList<>(Arrays.asList("comments"));
    private static final Integer testTaskID = 0;
    private TestUtils testUtils;
    private JdbcTemplate jdbcPostgresMasterTemplate;
    private JdbcTemplate jdbcPostgresCopyTemplate;
    private DatabaseSettings masterPostgresDatabaseSettings;
    private DatabaseSettings slavePostgresDatabaseSettings;
    private StorageSettings dropboxStorageSettings;
    private StorageSettings localFileSystemStorageSettings;
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
    void setJdbcPostgresCopyTemplate(JdbcTemplate jdbcPostgresCopyTemplate) {
        this.jdbcPostgresCopyTemplate = jdbcPostgresCopyTemplate;
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
    void setDropboxStorageSettings(StorageSettings dropboxStorageSettings) {
        this.dropboxStorageSettings = dropboxStorageSettings;
    }

    @Autowired
    void setLocalFileSystemStorageSettings(StorageSettings localFileSystemStorageSettings) {
        this.localFileSystemStorageSettings = localFileSystemStorageSettings;
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
        testUtils.clearDatabase(jdbcPostgresCopyTemplate);
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

        try (
                InputStream backupStream = databaseBackupManager.createBackup(masterPostgresDatabaseSettings, testTaskID)
        ) {
            BackupProperties backupProperties = backupPropertiesManager.initNewBackupProperties(
                    localFileSystemStorageSettings, processors, masterPostgresDatabaseSettings.getName());
            backupLoadManager.uploadBackup(backupStream, backupProperties, testTaskID);
            try (
                    InputStream downloadedBackup = backupLoadManager.downloadBackup(backupProperties, testTaskID)
            ) {
                databaseBackupManager.restoreBackup(downloadedBackup, slavePostgresDatabaseSettings, testTaskID);
            }
        }

        testUtils.compareLargeTables(tableNames, jdbcPostgresMasterTemplate, jdbcPostgresCopyTemplate);
    }

    @Test
    void whenCreatePostgresBackupAndUploadToDropboxAndRestore_databasesIsEqual() throws IOException {
        List<String> processors = new ArrayList<>();

        try (
                InputStream backupStream = databaseBackupManager.createBackup(masterPostgresDatabaseSettings, testTaskID)
        ) {
            BackupProperties backupProperties = backupPropertiesManager.initNewBackupProperties(
                    dropboxStorageSettings, processors, masterPostgresDatabaseSettings.getName());
            backupLoadManager.uploadBackup(backupStream, backupProperties, testTaskID);
            try (
                    InputStream downloadedBackup = backupLoadManager.downloadBackup(backupProperties, testTaskID)
            ) {
                databaseBackupManager.restoreBackup(downloadedBackup, slavePostgresDatabaseSettings, testTaskID);
            }
        }

        testUtils.compareLargeTables(tableNames, jdbcPostgresMasterTemplate, jdbcPostgresCopyTemplate);
    }

    @Test
    void whenCreatePostgresBackupAndCompressAndUploadToLocalFileSystemAndDecompressAndRestore_databasesIsEqual() throws IOException {
        List<String> processors = new ArrayList<>();
        processors.add("Compressor");

        try (
                InputStream backupStream = databaseBackupManager.createBackup(masterPostgresDatabaseSettings, testTaskID);
                InputStream compressedBackup = backupProcessorManager.process(backupStream, processors)
        ) {
            BackupProperties backupProperties = backupPropertiesManager.initNewBackupProperties(
                    localFileSystemStorageSettings, processors, masterPostgresDatabaseSettings.getName());
            backupLoadManager.uploadBackup(compressedBackup, backupProperties, testTaskID);
            try (
                    InputStream downloadedBackup = backupLoadManager.downloadBackup(backupProperties, testTaskID);
                    InputStream decompressedBackup = backupProcessorManager.deprocess(downloadedBackup, processors)
            ) {
                databaseBackupManager.restoreBackup(decompressedBackup, slavePostgresDatabaseSettings, testTaskID);
            }
        }

        testUtils.compareLargeTables(tableNames, jdbcPostgresMasterTemplate, jdbcPostgresCopyTemplate);
    }

    @Test
    void whenCreatePostgresBackupAndCompressAndUploadToDropboxAndDecompressAndRestore_databasesIsEqual() throws IOException {
        List<String> processors = new ArrayList<>();
        processors.add("Compressor");

        try (
                InputStream backupStream = databaseBackupManager.createBackup(masterPostgresDatabaseSettings, testTaskID);
                InputStream compressedBackup = backupProcessorManager.process(backupStream, processors)
        ) {
            BackupProperties backupProperties = backupPropertiesManager.initNewBackupProperties(
                    dropboxStorageSettings, processors, masterPostgresDatabaseSettings.getName());
            backupLoadManager.uploadBackup(compressedBackup, backupProperties, testTaskID);

            try (
                    InputStream downloadedBackup = backupLoadManager.downloadBackup(backupProperties, testTaskID);
                    InputStream decompressedBackup = backupProcessorManager.deprocess(downloadedBackup, processors)
            ) {
                databaseBackupManager.restoreBackup(decompressedBackup, slavePostgresDatabaseSettings, testTaskID);
            }
        }

        testUtils.compareLargeTables(tableNames, jdbcPostgresMasterTemplate, jdbcPostgresCopyTemplate);
    }

    @Test
    void whenCreateBackupAndUploadToDifferentStoragesAndRestoreEachIntoSeparateDatabases_databasesIsEqual() throws IOException {
        List<String> processors = new ArrayList<>();

        byte[] createdBackupAsByteArray;
        try (
                InputStream createdBackup = databaseBackupManager.createBackup(masterPostgresDatabaseSettings, testTaskID)
        ) {
            createdBackupAsByteArray = testUtils.getStreamCopyAsByteArray(createdBackup);
        }

        // get backup from local file system and restore
        {
            try (
                    InputStream inputStream = new ByteArrayInputStream(createdBackupAsByteArray)
            ) {
                BackupProperties backupProperties = backupPropertiesManager.initNewBackupProperties(
                        localFileSystemStorageSettings, processors, masterPostgresDatabaseSettings.getName());
                backupLoadManager.uploadBackup(inputStream, backupProperties, testTaskID);
                try (
                        InputStream downloadedBackup = backupLoadManager.downloadBackup(backupProperties, testTaskID)
                ) {
                    databaseBackupManager.restoreBackup(downloadedBackup, slavePostgresDatabaseSettings, testTaskID);
                }
                testUtils.compareLargeTables(tableNames, jdbcPostgresMasterTemplate, jdbcPostgresCopyTemplate);
            }
        }

        testUtils.clearDatabase(jdbcPostgresCopyTemplate);

        // get backup from dropbox and restore
        {
            try (
                    InputStream inputStream = new ByteArrayInputStream(createdBackupAsByteArray)
            ) {
                BackupProperties backupProperties = backupPropertiesManager.initNewBackupProperties(
                        dropboxStorageSettings, processors, masterPostgresDatabaseSettings.getName());
                backupLoadManager.uploadBackup(inputStream, backupProperties, testTaskID);
                try (
                        InputStream downloadedBackup = backupLoadManager.downloadBackup(backupProperties, testTaskID)
                ) {
                    databaseBackupManager.restoreBackup(downloadedBackup, slavePostgresDatabaseSettings, testTaskID);
                }
                testUtils.compareLargeTables(tableNames, jdbcPostgresMasterTemplate, jdbcPostgresCopyTemplate);
            }
        }
    }

    @Test
    void whenCreateBackupAndUploadToDifferentStoragesAndRestoreEachIntoSeparateDatabasesApplyingCompressor_databasesIsEqual() throws IOException {
        List<String> processors = new ArrayList<>();
        processors.add("compressor");

        byte[] createdBackupAsByteArray;
        try (
                InputStream createdBackup = databaseBackupManager.createBackup(masterPostgresDatabaseSettings, testTaskID);
                InputStream compressedBackup = backupProcessorManager.process(createdBackup, processors)
        ) {
            createdBackupAsByteArray = testUtils.getStreamCopyAsByteArray(compressedBackup);
        }

        // get backup from local file system and restore
        {
            try (
                    InputStream inputStream = new ByteArrayInputStream(createdBackupAsByteArray)
            ) {
                BackupProperties backupProperties = backupPropertiesManager.initNewBackupProperties(
                        localFileSystemStorageSettings, processors, masterPostgresDatabaseSettings.getName());
                backupLoadManager.uploadBackup(inputStream, backupProperties, testTaskID);
                try (
                        InputStream downloadedBackup = backupLoadManager.downloadBackup(backupProperties, testTaskID);
                        InputStream deprocessedBackup = backupProcessorManager.deprocess(downloadedBackup, processors)
                ) {
                    databaseBackupManager.restoreBackup(deprocessedBackup, slavePostgresDatabaseSettings, testTaskID);
                }
                testUtils.compareLargeTables(tableNames, jdbcPostgresMasterTemplate, jdbcPostgresCopyTemplate);
            }
        }

        testUtils.clearDatabase(jdbcPostgresCopyTemplate);

        // get backup from dropbox and restore
        {
            try (
                    InputStream inputStream = new ByteArrayInputStream(createdBackupAsByteArray)
            ) {
                BackupProperties backupProperties = backupPropertiesManager.initNewBackupProperties(
                        dropboxStorageSettings, processors, masterPostgresDatabaseSettings.getName());
                backupLoadManager.uploadBackup(inputStream, backupProperties, testTaskID);
                try (
                        InputStream downloadedBackup = backupLoadManager.downloadBackup(backupProperties, testTaskID);
                        InputStream deprocessedBackup = backupProcessorManager.deprocess(downloadedBackup, processors)
                ) {
                    databaseBackupManager.restoreBackup(deprocessedBackup, slavePostgresDatabaseSettings, testTaskID);
                }
                testUtils.compareLargeTables(tableNames, jdbcPostgresMasterTemplate, jdbcPostgresCopyTemplate);
            }
        }
    }

    @Test
    void givenAddMultipleTables_whenCreatePostgresBackupAndUploadToLocalFileSystemAndRestore_databasesIsEqual() throws IOException {
        List<String> processors = new ArrayList<>();

        jdbcPostgresMasterTemplate.execute("CREATE TABLE users" +
                "(" +
                "ID        SERIAL PRIMARY KEY," +
                "NAME    CHARACTER VARYING(36)   not null," +
                "DATE      TIMESTAMPTZ DEFAULT NOW()," +
                "INFO   CHARACTER VARYING(16000) not null" +
                ")");

        final long rowsToInsert = 10000L;
        jdbcPostgresMasterTemplate.update("insert into users (name, info)" +
                " select " +
                "    left(md5(i::text), 36)," +
                "    left(md5(random()::text), 16000) " +
                "from generate_series(0, ?) s(i)", rowsToInsert);

        try (
                InputStream backupStream = databaseBackupManager.createBackup(masterPostgresDatabaseSettings, testTaskID)
        ) {
            BackupProperties backupProperties =
                    backupPropertiesManager.initNewBackupProperties(
                            localFileSystemStorageSettings, processors, masterPostgresDatabaseSettings.getName());
            backupLoadManager.uploadBackup(backupStream, backupProperties, testTaskID);
            try (
                    InputStream downloadedBackup = backupLoadManager.downloadBackup(backupProperties, testTaskID)
            ) {
                databaseBackupManager.restoreBackup(downloadedBackup, slavePostgresDatabaseSettings, testTaskID);
            }
        }

        List<String> multipleTableNames = new ArrayList<>(tableNames);
        multipleTableNames.add("users");
        testUtils.compareLargeTables(multipleTableNames, jdbcPostgresMasterTemplate, jdbcPostgresCopyTemplate);
    }
}
