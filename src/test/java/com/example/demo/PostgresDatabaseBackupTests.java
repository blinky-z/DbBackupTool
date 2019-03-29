package com.example.demo;

import com.example.demo.entities.database.DatabaseSettings;
import com.example.demo.entities.storage.Storage;
import com.example.demo.entities.storage.StorageSettings;
import com.example.demo.manager.DatabaseBackupManager;
import com.example.demo.service.processor.BackupCompressor;
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
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest
public class PostgresDatabaseBackupTests extends ApplicationTests {
    private TestUtils testUtils;

    private JdbcTemplate jdbcMasterTemplate;

    private JdbcTemplate jdbcCopyTemplate;

    private DatabaseSettings masterDatabaseSettings;

    private DatabaseSettings copyDatabaseSettings;

    private DatabaseBackupManager databaseBackupManager;

    private BackupCompressor backupCompressor;

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
    public void setBackupCompressor(BackupCompressor backupCompressor) {
        this.backupCompressor = backupCompressor;
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
    public void createPostgresBackupAndUploadToLocalFileSystemAndRestore() {
        StorageSettings storageSettings = testUtils.buildStorageSettings(Storage.LOCAL_FILE_SYSTEM);

        InputStream createdBackup = databaseBackupManager.createBackup(masterDatabaseSettings);

        InputStream backupFromStorage = testUtils.uploadAndDownloadTextBackup(createdBackup, masterDatabaseSettings.getName(),
                storageSettings);

        databaseBackupManager.restoreBackup(backupFromStorage, copyDatabaseSettings);

        compareDatabases();
    }

    @Test
    public void createPostgresBackupAndUploadToDropboxAndRestore() {
        StorageSettings storageSettings = testUtils.buildStorageSettings(Storage.DROPBOX);

        try (
                InputStream createdBackup = databaseBackupManager.createBackup(masterDatabaseSettings);
                InputStream downloadedBackup = testUtils.uploadAndDownloadTextBackup(createdBackup, masterDatabaseSettings.getName(),
                        storageSettings);

        ) {
            databaseBackupManager.restoreBackup(downloadedBackup, copyDatabaseSettings);
        } catch (IOException e) {
            e.printStackTrace();
        }

        compareDatabases();
    }

    @Test
    public void createPostgresBackupAndCompressAndUploadToLocalFileSystemAndDecompressAndRestore() {
        StorageSettings storageSettings = testUtils.buildStorageSettings(Storage.LOCAL_FILE_SYSTEM);

        try (
                InputStream createdBackup = databaseBackupManager.createBackup(masterDatabaseSettings);
                InputStream compressedBackup = backupCompressor.compressBackup(createdBackup);
                InputStream downloadedCompressedBackup = testUtils.uploadAndDownloadBinaryBackup(compressedBackup,
                        masterDatabaseSettings.getName(), storageSettings);
                InputStream decompressedBackup = backupCompressor.decompressBackup(downloadedCompressedBackup);
        ) {
            databaseBackupManager.restoreBackup(decompressedBackup, copyDatabaseSettings);
        } catch (IOException e) {
            e.printStackTrace();
        }

        compareDatabases();
    }

    @Test
    public void createPostgresBackupAndCompressAndUploadToDropboxAndDecompressAndRestore() {
        StorageSettings storageSettings = testUtils.buildStorageSettings(Storage.DROPBOX);

        try (
                InputStream createdBackup = databaseBackupManager.createBackup(masterDatabaseSettings);
                InputStream compressedBackup = backupCompressor.compressBackup(createdBackup);
                InputStream downloadedCompressedBackup = testUtils.uploadAndDownloadBinaryBackup(compressedBackup,
                        masterDatabaseSettings.getName(), storageSettings);
                InputStream decompressedBackup = backupCompressor.decompressBackup(downloadedCompressedBackup);
        ) {
            databaseBackupManager.restoreBackup(decompressedBackup, copyDatabaseSettings);
        } catch (IOException e) {
            e.printStackTrace();
        }

        compareDatabases();
    }

    @Test
    public void createPostgresBackupAndUploadToDifferentStoragesAndRestoreEachIntoSeparateDatabases() {
        StorageSettings localFileSystemStorageSettings = testUtils.buildStorageSettings(Storage.LOCAL_FILE_SYSTEM);
        StorageSettings dropboxStorageSettings = testUtils.buildStorageSettings(Storage.DROPBOX);

        InputStream createdBackup = databaseBackupManager.createBackup(masterDatabaseSettings);
        byte[] createdBackupAsByteArray = testUtils.getStreamCopyAsByteArray(createdBackup);
        try {
            createdBackup.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        {
            try (
                    InputStream inputStream = new ByteArrayInputStream(createdBackupAsByteArray);
                    InputStream downloadedUncompressedInputStream = testUtils.uploadAndDownloadTextBackup(
                            inputStream, masterDatabaseSettings.getName(), localFileSystemStorageSettings);
            ) {
                databaseBackupManager.restoreBackup(downloadedUncompressedInputStream, copyDatabaseSettings);
                compareDatabases();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        testUtils.clearDatabase(jdbcCopyTemplate);

        {
            try (
                    InputStream inputStream = new ByteArrayInputStream(createdBackupAsByteArray);
                    InputStream compressedInputStream = backupCompressor.compressBackup(inputStream);
                    InputStream downloadedCompressedInputStream = testUtils.uploadAndDownloadBinaryBackup(
                            compressedInputStream, masterDatabaseSettings.getName(), dropboxStorageSettings);
                    InputStream decompressedDownloadedInputStream = backupCompressor.decompressBackup(downloadedCompressedInputStream);
            ) {
                databaseBackupManager.restoreBackup(decompressedDownloadedInputStream, copyDatabaseSettings);
                compareDatabases();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
