package com.example.demo;

import com.example.demo.entities.database.DatabaseSettings;
import com.example.demo.entities.storage.Storage;
import com.example.demo.entities.storage.StorageSettings;
import com.example.demo.manager.DatabaseBackupManager;
import com.example.demo.service.processor.BackupCompressor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest
public class DropboxStorageTests extends ApplicationTests {
    private TestUtils testUtils;

    private DatabaseBackupManager databaseBackupManager;

    private JdbcTemplate jdbcMasterTemplate;

    private DatabaseSettings masterDatabaseSettings;

    private BackupCompressor backupCompressor;

    @Autowired
    public void setTestUtils(TestUtils testUtils) {
        this.testUtils = testUtils;
    }

    @Autowired
    public void setMasterDatabaseSettings(DatabaseSettings masterDatabaseSettings) {
        this.masterDatabaseSettings = masterDatabaseSettings;
    }

    @Autowired
    public void setDatabaseBackupManager(DatabaseBackupManager databaseBackupManager) {
        this.databaseBackupManager = databaseBackupManager;
    }

    @Autowired
    public void setJdbcMasterTemplate(JdbcTemplate jdbcMasterTemplate) {
        this.jdbcMasterTemplate = jdbcMasterTemplate;
    }

    @Autowired
    public void setBackupCompressor(BackupCompressor backupCompressor) {
        this.backupCompressor = backupCompressor;
    }

    @Test
    public void whenUploadTextBackupAndDownload_contentIsEqual() {
        testUtils.clearDatabase(jdbcMasterTemplate);
        testUtils.initDatabase(jdbcMasterTemplate);

        InputStream backupStream = databaseBackupManager.createBackup(masterDatabaseSettings);
        byte[] streamContent = testUtils.getStreamCopyAsByteArray(backupStream);

        InputStream inputStream = new ByteArrayInputStream(streamContent);
        InputStream copyInputStream = new ByteArrayInputStream(streamContent);

        StorageSettings storageSettings = testUtils.buildStorageSettings(Storage.DROPBOX);
        InputStream downloadedBackup = testUtils.uploadAndDownloadTextBackup(inputStream, masterDatabaseSettings.getName(),
                storageSettings);

        assertTrue(testUtils.streamsContentEquals(copyInputStream, downloadedBackup));
    }

    @Test
    public void whenUploadCompressedBackupAndDownload_contentIsEqual() {
        testUtils.clearDatabase(jdbcMasterTemplate);
        testUtils.initDatabase(jdbcMasterTemplate);

        InputStream backupStream = databaseBackupManager.createBackup(masterDatabaseSettings);

        InputStream compressedBackup = backupCompressor.compressBackup(backupStream);

        byte[] compressedBackupContent = testUtils.getStreamCopyAsByteArray(compressedBackup);
        InputStream inputStream = new ByteArrayInputStream(compressedBackupContent);
        InputStream copyCompressedBackup = new ByteArrayInputStream(compressedBackupContent);

        StorageSettings storageSettings = testUtils.buildStorageSettings(Storage.DROPBOX);
        InputStream downloadedBackup = testUtils.uploadAndDownloadBinaryBackup(inputStream, masterDatabaseSettings.getName(),
                storageSettings);

        assertTrue(testUtils.streamsContentEquals(copyCompressedBackup, downloadedBackup));
    }

    @Test
    public void whenUploadCompressedBackupAndDownloadAndDecompress_contentIsEqualToSource() {
        testUtils.clearDatabase(jdbcMasterTemplate);
        testUtils.initDatabase(jdbcMasterTemplate);

        InputStream backupStream = databaseBackupManager.createBackup(masterDatabaseSettings);

        byte[] sourceBackupContent = testUtils.getStreamCopyAsByteArray(backupStream);
        InputStream inputStream = new ByteArrayInputStream(sourceBackupContent);
        InputStream copySourceBackup = new ByteArrayInputStream(sourceBackupContent);

        InputStream compressedBackup = backupCompressor.compressBackup(inputStream);

        StorageSettings storageSettings = testUtils.buildStorageSettings(Storage.DROPBOX);
        InputStream downloadedBackup = testUtils.uploadAndDownloadBinaryBackup(compressedBackup, masterDatabaseSettings.getName(),
                storageSettings);

        InputStream decompressedBackup = backupCompressor.decompressBackup(downloadedBackup);

        assertTrue(testUtils.streamsContentEquals(copySourceBackup, decompressedBackup));
    }
}
