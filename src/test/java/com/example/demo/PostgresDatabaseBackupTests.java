package com.example.demo;

import com.example.demo.entities.backup.BackupProperties;
import com.example.demo.entities.database.DatabaseSettings;
import com.example.demo.entities.storage.StorageSettings;
import com.example.demo.manager.BackupLoadManager;
import com.example.demo.manager.BackupProcessorManager;
import com.example.demo.manager.DatabaseBackupManager;
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
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

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

    @Before
    public void initDatabase() {
        testUtils.clearDatabase(jdbcPostgresMasterTemplate);
        testUtils.clearDatabase(jdbcPostgresCopyTemplate);
        testUtils.initDatabase(jdbcPostgresMasterTemplate);
    }

    private void compareDatabases() {
        long startRangeId = 0;
        long rowsPerRequest = 10000;
        long rowsToInsert = 10000L;
        while (startRangeId < rowsToInsert) {
            long endRangeId = startRangeId + rowsPerRequest;
            List<Map<String, Object>> oldData = jdbcPostgresMasterTemplate.queryForList(
                    "SELECT * FROM comments WHERE id BETWEEN ? AND ?",
                    startRangeId, endRangeId);
            List<Map<String, Object>> restoredData = jdbcPostgresCopyTemplate.queryForList(
                    "SELECT * FROM comments WHERE id BETWEEN ? AND ?",
                    startRangeId, endRangeId);
            startRangeId = endRangeId;

            assertEquals(oldData, restoredData);
        }
    }

    @Test
    public void whenCreatePostgresBackupAndUploadToLocalFileSystemAndRestore_databasesIsEqual() {
        List<String> processors = new ArrayList<>();

        try (
                InputStream backupStream = databaseBackupManager.createBackup(masterPostgresDatabaseSettings)
        ) {
            BackupProperties backupProperties = backupLoadManager.uploadBackup(backupStream, localFileSystemStorageSettings,
                    processors, masterPostgresDatabaseSettings.getName());
            try (
                    InputStream downloadedBackup = backupLoadManager.downloadBackup(localFileSystemStorageSettings,
                            backupProperties);
            ) {
                databaseBackupManager.restoreBackup(downloadedBackup, copyPostgresDatabaseSettings);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        compareDatabases();
    }

    @Test
    public void whenCreatePostgresBackupAndUploadToDropboxAndRestore_databasesIsEqual() {
        List<String> processors = new ArrayList<>();

        try (
                InputStream backupStream = databaseBackupManager.createBackup(masterPostgresDatabaseSettings);
        ) {
            BackupProperties backupProperties = backupLoadManager.uploadBackup(backupStream, dropboxStorageSettings, processors,
                    masterPostgresDatabaseSettings.getName());
            try (
                    InputStream downloadedBackup = backupLoadManager.downloadBackup(dropboxStorageSettings, backupProperties)
            ) {
                databaseBackupManager.restoreBackup(downloadedBackup, copyPostgresDatabaseSettings);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        compareDatabases();
    }

    @Test
    public void whenCreatePostgresBackupAndCompressAndUploadToLocalFileSystemAndDecompressAndRestore_databasesIsEqual() {
        List<String> processors = new ArrayList<>();
        processors.add("Compressor");

        try (
                InputStream backupStream = databaseBackupManager.createBackup(masterPostgresDatabaseSettings);
                InputStream compressedBackup = backupProcessorManager.process(backupStream, processors);
        ) {
            BackupProperties backupProperties = backupLoadManager.uploadBackup(compressedBackup, localFileSystemStorageSettings,
                    processors, masterPostgresDatabaseSettings.getName());
            try (
                    InputStream downloadedBackup = backupLoadManager.downloadBackup(localFileSystemStorageSettings,
                            backupProperties);
                    InputStream decompressedBackup = backupProcessorManager.deprocess(downloadedBackup, processors);
            ) {
                databaseBackupManager.restoreBackup(decompressedBackup, copyPostgresDatabaseSettings);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        compareDatabases();
    }

    @Test
    public void whenCreatePostgresBackupAndCompressAndUploadToDropboxAndDecompressAndRestore_databasesIsEqual() {
        List<String> processors = new ArrayList<>();
        processors.add("Compressor");

        try (
                InputStream backupStream = databaseBackupManager.createBackup(masterPostgresDatabaseSettings);
                InputStream compressedBackup = backupProcessorManager.process(backupStream, processors);
        ) {
            BackupProperties backupProperties = backupLoadManager.uploadBackup(compressedBackup, dropboxStorageSettings, processors,
                    masterPostgresDatabaseSettings.getName());
            try (
                    InputStream downloadedBackup = backupLoadManager.downloadBackup(dropboxStorageSettings, backupProperties);
                    InputStream decompressedBackup = backupProcessorManager.deprocess(downloadedBackup, processors);
            ) {
                databaseBackupManager.restoreBackup(decompressedBackup, copyPostgresDatabaseSettings);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        compareDatabases();
    }

    @Test
    public void whenCreatePostgresBackupAndUploadToDifferentStoragesAndRestoreEachIntoSeparateDatabases_databasesIsEqual() {
        List<String> processors = new ArrayList<>();

        byte[] createdBackupAsByteArray;
        try (
                InputStream createdBackup = databaseBackupManager.createBackup(masterPostgresDatabaseSettings)
        ) {
            createdBackupAsByteArray = testUtils.getStreamCopyAsByteArray(createdBackup);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException();
        }

        // get backup from local file system and restore
        {
            try (
                    InputStream inputStream = new ByteArrayInputStream(createdBackupAsByteArray);
            ) {
                BackupProperties backupProperties = backupLoadManager.uploadBackup(inputStream, localFileSystemStorageSettings,
                        processors, masterPostgresDatabaseSettings.getName());
                try (
                        InputStream downloadedBackup = backupLoadManager.downloadBackup(localFileSystemStorageSettings,
                                backupProperties);
                ) {
                    databaseBackupManager.restoreBackup(downloadedBackup, copyPostgresDatabaseSettings);
                }
                compareDatabases();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        testUtils.clearDatabase(jdbcPostgresCopyTemplate);

        // get backup from dropbox and restore
        {
            try (
                    InputStream inputStream = new ByteArrayInputStream(createdBackupAsByteArray);
            ) {
                BackupProperties backupProperties = backupLoadManager.uploadBackup(inputStream, dropboxStorageSettings,
                        processors, masterPostgresDatabaseSettings.getName());
                try (
                        InputStream downloadedBackup = backupLoadManager.downloadBackup(dropboxStorageSettings, backupProperties);
                ) {
                    databaseBackupManager.restoreBackup(downloadedBackup, copyPostgresDatabaseSettings);
                }
                compareDatabases();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
