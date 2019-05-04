package com.blog.DatabaseBackupServiceTests;

import com.blog.ApplicationTests;
import com.blog.TestUtils;
import com.blog.entities.backup.BackupProperties;
import com.blog.entities.database.DatabaseSettings;
import com.blog.entities.storage.StorageSettings;
import com.blog.manager.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
public class PostgresDatabaseBackupTests extends ApplicationTests {
    private TestUtils testUtils;

    private JdbcTemplate jdbcPostgresMasterTemplate;

    private JdbcTemplate jdbcPostgresCopyTemplate;

    private DatabaseSettings masterPostgresDatabaseSettings;

    private DatabaseSettings copyPostgresDatabaseSettings;

    private StorageSettings dropboxStorageSettings;

    private StorageSettings localFileSystemStorageSettings;

    private DatabaseBackupManager databaseBackupManager;

    private BackupProcessorManager backupProcessorManager;

    private BackupLoadManager backupLoadManager;

    private StorageSettingsManager storageSettingsManager;

    private DatabaseSettingsManager databaseSettingsManager;

    @Autowired
    public void setTestUtils(TestUtils testUtils) {
        this.testUtils = testUtils;
    }

    @Autowired
    public void setJdbcPostgresMasterTemplate(JdbcTemplate jdbcPostgresMasterTemplate) {
        this.jdbcPostgresMasterTemplate = jdbcPostgresMasterTemplate;
    }

    @Autowired
    public void setJdbcPostgresCopyTemplate(JdbcTemplate jdbcPostgresCopyTemplate) {
        this.jdbcPostgresCopyTemplate = jdbcPostgresCopyTemplate;
    }

    @Autowired
    public void setMasterPostgresDatabaseSettings(DatabaseSettings masterPostgresDatabaseSettings) {
        this.masterPostgresDatabaseSettings = masterPostgresDatabaseSettings;
    }

    @Autowired
    public void setCopyPostgresDatabaseSettings(DatabaseSettings copyPostgresDatabaseSettings) {
        this.copyPostgresDatabaseSettings = copyPostgresDatabaseSettings;
    }

    @Autowired
    public void setDropboxStorageSettings(StorageSettings dropboxStorageSettings) {
        this.dropboxStorageSettings = dropboxStorageSettings;
    }

    @Autowired
    public void setLocalFileSystemStorageSettings(StorageSettings localFileSystemStorageSettings) {
        this.localFileSystemStorageSettings = localFileSystemStorageSettings;
    }

    @Autowired
    public void setDatabaseBackupManager(DatabaseBackupManager databaseBackupManager) {
        this.databaseBackupManager = databaseBackupManager;
    }

    @Autowired
    public void setBackupProcessorManager(BackupProcessorManager backupProcessorManager) {
        this.backupProcessorManager = backupProcessorManager;
    }

    @Autowired
    public void setBackupLoadManager(BackupLoadManager backupLoadManager) {
        this.backupLoadManager = backupLoadManager;
    }

    @Autowired
    public void setStorageSettingsManager(StorageSettingsManager storageSettingsManager) {
        this.storageSettingsManager = storageSettingsManager;
    }

    @Autowired
    public void setDatabaseSettingsManager(DatabaseSettingsManager databaseSettingsManager) {
        this.databaseSettingsManager = databaseSettingsManager;
    }

    @Before
    public void init() {
        testUtils.clearDatabase(jdbcPostgresMasterTemplate);
        testUtils.clearDatabase(jdbcPostgresCopyTemplate);
        storageSettingsManager.save(dropboxStorageSettings);
        storageSettingsManager.save(localFileSystemStorageSettings);
        databaseSettingsManager.save(masterPostgresDatabaseSettings);
        databaseSettingsManager.save(copyPostgresDatabaseSettings);
        addTables(jdbcPostgresMasterTemplate);
    }

    private static final List<String> tableNames = new ArrayList<>(Arrays.asList("comments"));

    public void addTables(JdbcTemplate jdbcTemplate) {
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
    public void whenCreatePostgresBackupAndUploadToLocalFileSystemAndRestore_databasesIsEqual() throws IOException {
        List<String> processors = new ArrayList<>();

        try (
                InputStream backupStream = databaseBackupManager.createBackup(masterPostgresDatabaseSettings)
        ) {
            BackupProperties backupProperties = backupLoadManager.uploadBackup(backupStream, localFileSystemStorageSettings,
                    processors, masterPostgresDatabaseSettings.getName());
            assert (storageSettingsManager.existsById(backupProperties.getStorageSettingsName()));
            try (
                    InputStream downloadedBackup = backupLoadManager.downloadBackup(backupProperties)
            ) {
                databaseBackupManager.restoreBackup(downloadedBackup, copyPostgresDatabaseSettings);
            }
        }

        testUtils.compareLargeTables(tableNames, jdbcPostgresMasterTemplate, jdbcPostgresCopyTemplate);
    }

    @Test
    public void whenCreatePostgresBackupAndUploadToDropboxAndRestore_databasesIsEqual() throws IOException {
        List<String> processors = new ArrayList<>();

        try (
                InputStream backupStream = databaseBackupManager.createBackup(masterPostgresDatabaseSettings)
        ) {
            BackupProperties backupProperties = backupLoadManager.uploadBackup(backupStream, dropboxStorageSettings, processors,
                    masterPostgresDatabaseSettings.getName());
            try (
                    InputStream downloadedBackup = backupLoadManager.downloadBackup(backupProperties)
            ) {
                databaseBackupManager.restoreBackup(downloadedBackup, copyPostgresDatabaseSettings);
            }
        }

        testUtils.compareLargeTables(tableNames, jdbcPostgresMasterTemplate, jdbcPostgresCopyTemplate);
    }

    @Test
    public void whenCreatePostgresBackupAndCompressAndUploadToLocalFileSystemAndDecompressAndRestore_databasesIsEqual() throws IOException {
        List<String> processors = new ArrayList<>();
        processors.add("Compressor");

        try (
                InputStream backupStream = databaseBackupManager.createBackup(masterPostgresDatabaseSettings);
                InputStream compressedBackup = backupProcessorManager.process(backupStream, processors)
        ) {
            BackupProperties backupProperties = backupLoadManager.uploadBackup(compressedBackup, localFileSystemStorageSettings,
                    processors, masterPostgresDatabaseSettings.getName());
            try (
                    InputStream downloadedBackup = backupLoadManager.downloadBackup(backupProperties);
                    InputStream decompressedBackup = backupProcessorManager.deprocess(downloadedBackup, processors)
            ) {
                databaseBackupManager.restoreBackup(decompressedBackup, copyPostgresDatabaseSettings);
            }
        }

        testUtils.compareLargeTables(tableNames, jdbcPostgresMasterTemplate, jdbcPostgresCopyTemplate);
    }

    @Test
    public void whenCreatePostgresBackupAndCompressAndUploadToDropboxAndDecompressAndRestore_databasesIsEqual() throws IOException {
        List<String> processors = new ArrayList<>();
        processors.add("Compressor");

        try (
                InputStream backupStream = databaseBackupManager.createBackup(masterPostgresDatabaseSettings);
                InputStream compressedBackup = backupProcessorManager.process(backupStream, processors)
        ) {
            BackupProperties backupProperties = backupLoadManager.uploadBackup(compressedBackup, dropboxStorageSettings,
                    processors, masterPostgresDatabaseSettings.getName());

            try (
                    InputStream downloadedBackup = backupLoadManager.downloadBackup(backupProperties);
                    InputStream decompressedBackup = backupProcessorManager.deprocess(downloadedBackup, processors)
            ) {
                databaseBackupManager.restoreBackup(decompressedBackup, copyPostgresDatabaseSettings);
            }
        }

        testUtils.compareLargeTables(tableNames, jdbcPostgresMasterTemplate, jdbcPostgresCopyTemplate);
    }

    @Test
    public void whenCreateBackupAndUploadToDifferentStoragesAndRestoreEachIntoSeparateDatabases_databasesIsEqual() throws IOException {
        List<String> processors = new ArrayList<>();

        byte[] createdBackupAsByteArray;
        try (
                InputStream createdBackup = databaseBackupManager.createBackup(masterPostgresDatabaseSettings)
        ) {
            createdBackupAsByteArray = testUtils.getStreamCopyAsByteArray(createdBackup);
        }

        // get backup from local file system and restore
        {
            try (
                    InputStream inputStream = new ByteArrayInputStream(createdBackupAsByteArray)
            ) {
                BackupProperties backupProperties = backupLoadManager.uploadBackup(inputStream, localFileSystemStorageSettings,
                        processors, masterPostgresDatabaseSettings.getName());
                try (
                        InputStream downloadedBackup = backupLoadManager.downloadBackup(backupProperties)
                ) {
                    databaseBackupManager.restoreBackup(downloadedBackup, copyPostgresDatabaseSettings);
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
                BackupProperties backupProperties = backupLoadManager.uploadBackup(inputStream, dropboxStorageSettings,
                        processors, masterPostgresDatabaseSettings.getName());
                try (
                        InputStream downloadedBackup = backupLoadManager.downloadBackup(backupProperties)
                ) {
                    databaseBackupManager.restoreBackup(downloadedBackup, copyPostgresDatabaseSettings);
                }
                testUtils.compareLargeTables(tableNames, jdbcPostgresMasterTemplate, jdbcPostgresCopyTemplate);
            }
        }
    }

    @Test
    public void whenCreateBackupAndUploadToDifferentStoragesAndRestoreEachIntoSeparateDatabasesApplyingCompressor_databasesIsEqual() throws IOException {
        List<String> processors = new ArrayList<>();
        processors.add("compressor");

        byte[] createdBackupAsByteArray;
        try (
                InputStream createdBackup = databaseBackupManager.createBackup(masterPostgresDatabaseSettings);
                InputStream compressedBackup = backupProcessorManager.process(createdBackup, processors)
        ) {
            createdBackupAsByteArray = testUtils.getStreamCopyAsByteArray(compressedBackup);
        }

        // get backup from local file system and restore
        {
            try (
                    InputStream inputStream = new ByteArrayInputStream(createdBackupAsByteArray)
            ) {
                BackupProperties backupProperties = backupLoadManager.uploadBackup(inputStream, localFileSystemStorageSettings,
                        processors, masterPostgresDatabaseSettings.getName());
                try (
                        InputStream downloadedBackup = backupLoadManager.downloadBackup(backupProperties);
                        InputStream deprocessedBackup = backupProcessorManager.deprocess(downloadedBackup, processors)
                ) {
                    databaseBackupManager.restoreBackup(deprocessedBackup, copyPostgresDatabaseSettings);
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
                BackupProperties backupProperties = backupLoadManager.uploadBackup(inputStream, dropboxStorageSettings,
                        processors, masterPostgresDatabaseSettings.getName());
                try (
                        InputStream downloadedBackup = backupLoadManager.downloadBackup(backupProperties);
                        InputStream deprocessedBackup = backupProcessorManager.deprocess(downloadedBackup, processors)
                ) {
                    databaseBackupManager.restoreBackup(deprocessedBackup, copyPostgresDatabaseSettings);
                }
                testUtils.compareLargeTables(tableNames, jdbcPostgresMasterTemplate, jdbcPostgresCopyTemplate);
            }
        }
    }

    @Test
    public void givenAddMultipleTables_whenCreatePostgresBackupAndUploadToLocalFileSystemAndRestore_databasesIsEqual() throws IOException {
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
                InputStream backupStream = databaseBackupManager.createBackup(masterPostgresDatabaseSettings)
        ) {
            BackupProperties backupProperties = backupLoadManager.uploadBackup(backupStream, localFileSystemStorageSettings,
                    processors, masterPostgresDatabaseSettings.getName());
            try (
                    InputStream downloadedBackup = backupLoadManager.downloadBackup(backupProperties)
            ) {
                databaseBackupManager.restoreBackup(downloadedBackup, copyPostgresDatabaseSettings);
            }
        }

        List<String> multipleTableNames = new ArrayList<>(tableNames);
        multipleTableNames.add("users");
        testUtils.compareLargeTables(multipleTableNames, jdbcPostgresMasterTemplate, jdbcPostgresCopyTemplate);
    }
}
