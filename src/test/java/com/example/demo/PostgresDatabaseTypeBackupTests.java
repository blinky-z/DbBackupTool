package com.example.demo;

import com.example.demo.entities.backup.BackupProperties;
import com.example.demo.entities.database.DatabaseSettings;
import com.example.demo.entities.storage.StorageSettings;
import com.example.demo.entities.storage.StorageType;
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
public class PostgresDatabaseTypeBackupTests extends ApplicationTests {
    private TestUtils testUtils;

    private JdbcTemplate jdbcMasterTemplate;

    private JdbcTemplate jdbcCopyTemplate;

    private DatabaseSettings masterDatabaseSettings;

    private DatabaseSettings copyDatabaseSettings;

    private DatabaseBackupManager databaseBackupManager;

    private BackupProcessorManager backupProcessorManager;

    private BackupLoadManager backupLoadManager;

    @Autowired
    public void setTestUtils(TestUtils testUtils) {
        this.testUtils = testUtils;
    }

    @Autowired
    public void setJdbcMasterTemplate(JdbcTemplate jdbcMasterTemplate) {
        this.jdbcMasterTemplate = jdbcMasterTemplate;
    }

    @Autowired
    public void setJdbcCopyTemplate(JdbcTemplate jdbcCopyTemplate) {
        this.jdbcCopyTemplate = jdbcCopyTemplate;
    }

    @Autowired
    public void setMasterDatabaseSettings(DatabaseSettings masterDatabaseSettings) {
        this.masterDatabaseSettings = masterDatabaseSettings;
    }

    @Autowired
    public void setCopyDatabaseSettings(DatabaseSettings copyDatabaseSettings) {
        this.copyDatabaseSettings = copyDatabaseSettings;
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

    private static long rowsToInsert = 10000L;

    @Before
    public void initDatabase() {
        testUtils.clearDatabase(jdbcMasterTemplate);
        testUtils.clearDatabase(jdbcCopyTemplate);
        testUtils.initDatabase(jdbcMasterTemplate);
    }

    private void compareDatabases() {
        long startRangeId = 0;
        long rowsPerRequest = 10000;
        while (startRangeId < rowsToInsert) {
            long endRangeId = startRangeId + rowsPerRequest;
            List<Map<String, Object>> oldData = jdbcMasterTemplate.queryForList(
                    "SELECT * FROM comments WHERE id BETWEEN ? AND ?",
                    startRangeId, endRangeId);
            List<Map<String, Object>> restoredData = jdbcCopyTemplate.queryForList(
                    "SELECT * FROM comments WHERE id BETWEEN ? AND ?",
                    startRangeId, endRangeId);
            startRangeId = endRangeId;

            assertEquals(oldData, restoredData);
        }
    }

    @Test
    public void whenCreatePostgresBackupAndUploadToLocalFileSystemAndRestore_databasesIsEqual() {
        StorageSettings storageSettings = testUtils.buildStorageSettings(StorageType.LOCAL_FILE_SYSTEM);

        List<String> processors = new ArrayList<>();

        try (
                InputStream backupStream = databaseBackupManager.createBackup(masterDatabaseSettings)
        ) {
            BackupProperties backupProperties = backupLoadManager.uploadBackup(backupStream, storageSettings, processors,
                    masterDatabaseSettings.getName());
            try (
                    InputStream downloadedBackup = backupLoadManager.downloadBackup(storageSettings, backupProperties);
            ) {
                databaseBackupManager.restoreBackup(downloadedBackup, copyDatabaseSettings);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        compareDatabases();
    }

    @Test
    public void whenCreatePostgresBackupAndUploadToDropboxAndRestore_databasesIsEqual() {
        StorageSettings storageSettings = testUtils.buildStorageSettings(StorageType.DROPBOX);

        List<String> processors = new ArrayList<>();

        try (
                InputStream backupStream = databaseBackupManager.createBackup(masterDatabaseSettings);
        ) {
            BackupProperties backupProperties = backupLoadManager.uploadBackup(backupStream, storageSettings, processors,
                    masterDatabaseSettings.getName());
            try (
                    InputStream downloadedBackup = backupLoadManager.downloadBackup(storageSettings, backupProperties)
            ) {
                databaseBackupManager.restoreBackup(downloadedBackup, copyDatabaseSettings);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        compareDatabases();
    }

    @Test
    public void whenCreatePostgresBackupAndCompressAndUploadToLocalFileSystemAndDecompressAndRestore_databasesIsEqual() {
        StorageSettings storageSettings = testUtils.buildStorageSettings(StorageType.LOCAL_FILE_SYSTEM);

        List<String> processors = new ArrayList<>();
        processors.add("Compressor");

        try (
                InputStream backupStream = databaseBackupManager.createBackup(masterDatabaseSettings);
                InputStream compressedBackup = backupProcessorManager.process(backupStream, processors);
        ) {
            BackupProperties backupProperties = backupLoadManager.uploadBackup(compressedBackup, storageSettings, processors,
                    masterDatabaseSettings.getName());
            try (
                    InputStream downloadedBackup = backupLoadManager.downloadBackup(storageSettings, backupProperties);
                    InputStream decompressedBackup = backupProcessorManager.deprocess(downloadedBackup, processors);
            ) {
                databaseBackupManager.restoreBackup(decompressedBackup, copyDatabaseSettings);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        compareDatabases();
    }

    @Test
    public void whenCreatePostgresBackupAndCompressAndUploadToDropboxAndDecompressAndRestore_databasesIsEqual() {
        StorageSettings storageSettings = testUtils.buildStorageSettings(StorageType.DROPBOX);

        List<String> processors = new ArrayList<>();
        processors.add("Compressor");

        try (
                InputStream backupStream = databaseBackupManager.createBackup(masterDatabaseSettings);
                InputStream compressedBackup = backupProcessorManager.process(backupStream, processors);
        ) {
            BackupProperties backupProperties = backupLoadManager.uploadBackup(compressedBackup, storageSettings, processors,
                    masterDatabaseSettings.getName());
            try (
                    InputStream downloadedBackup = backupLoadManager.downloadBackup(storageSettings, backupProperties);
                    InputStream decompressedBackup = backupProcessorManager.deprocess(downloadedBackup, processors);
            ) {
                databaseBackupManager.restoreBackup(decompressedBackup, copyDatabaseSettings);
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
                InputStream createdBackup = databaseBackupManager.createBackup(masterDatabaseSettings)
        ) {
            createdBackupAsByteArray = testUtils.getStreamCopyAsByteArray(createdBackup);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException();
        }

        // get backup from local file system and restore
        {
            StorageSettings localFileSystemStorageSettings = testUtils.buildStorageSettings(StorageType.LOCAL_FILE_SYSTEM);
            try (
                    InputStream inputStream = new ByteArrayInputStream(createdBackupAsByteArray);
            ) {
                BackupProperties backupProperties = backupLoadManager.uploadBackup(inputStream, localFileSystemStorageSettings,
                        processors, masterDatabaseSettings.getName());
                try (
                        InputStream downloadedBackup = backupLoadManager.downloadBackup(localFileSystemStorageSettings,
                                backupProperties);
                ) {
                    databaseBackupManager.restoreBackup(downloadedBackup, copyDatabaseSettings);
                }
                compareDatabases();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        testUtils.clearDatabase(jdbcCopyTemplate);

        // get backup from dropbox and restore
        {
            StorageSettings dropboxStorageSettings = testUtils.buildStorageSettings(StorageType.DROPBOX);
            try (
                    InputStream inputStream = new ByteArrayInputStream(createdBackupAsByteArray);
            ) {
                BackupProperties backupProperties = backupLoadManager.uploadBackup(inputStream, dropboxStorageSettings,
                        processors, masterDatabaseSettings.getName());
                try (
                        InputStream downloadedBackup = backupLoadManager.downloadBackup(dropboxStorageSettings, backupProperties);
                ) {
                    databaseBackupManager.restoreBackup(downloadedBackup, copyDatabaseSettings);
                }
                compareDatabases();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
