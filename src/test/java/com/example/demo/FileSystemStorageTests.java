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

import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest
public class FileSystemStorageTests extends ApplicationTests {
    private BackupCompressor backupCompressor;

    private TestUtils testUtils;

    private DatabaseBackupManager databaseBackupManager;

    private JdbcTemplate jdbcMasterTemplate;

    private DatabaseSettings masterDatabaseSettings;

    @Autowired
    public void setTestUtils(TestUtils testUtils) {
        this.testUtils = testUtils;
    }

    @Autowired
    public void setBackupCompressor(BackupCompressor backupCompressor) {
        this.backupCompressor = backupCompressor;
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

    @Before
    public void setUp() {
        testUtils.clearDatabase(jdbcMasterTemplate);
        testUtils.initDatabase(jdbcMasterTemplate);
    }

    @Test
    public void whenUploadTextBackupAndDownload_contentIsEqual() {
        StorageSettings storageSettings = testUtils.buildStorageSettings(Storage.LOCAL_FILE_SYSTEM);

        try (
                InputStream backupStream = databaseBackupManager.createBackup(masterDatabaseSettings)
        ) {
            byte[] streamContent = testUtils.getStreamCopyAsByteArray(backupStream);
            try (
                    InputStream inputStream = new ByteArrayInputStream(streamContent);
                    InputStream copyInputStream = new ByteArrayInputStream(streamContent);
                    InputStream downloadedBackup = testUtils.uploadAndDownloadTextBackup(copyInputStream, masterDatabaseSettings.getName(),
                            storageSettings)
            ) {
                assertTrue(testUtils.streamsContentEquals(inputStream, downloadedBackup));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void whenUploadCompressedBackupAndDownload_contentIsEqual() {
        StorageSettings storageSettings = testUtils.buildStorageSettings(Storage.LOCAL_FILE_SYSTEM);

        try (
                InputStream backupStream = databaseBackupManager.createBackup(masterDatabaseSettings);
                InputStream compressedBackup = backupCompressor.compressBackup(backupStream)
        ) {
            byte[] compressedBackupContent = testUtils.getStreamCopyAsByteArray(compressedBackup);
            try (
                    InputStream inputStream = new ByteArrayInputStream(compressedBackupContent);
                    InputStream copyInputStream = new ByteArrayInputStream(compressedBackupContent);
                    InputStream downloadedCompressedBackup = testUtils.uploadAndDownloadBinaryBackup(copyInputStream,
                            masterDatabaseSettings.getName(), storageSettings)
            ) {
                assertTrue(testUtils.streamsContentEquals(inputStream, downloadedCompressedBackup));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void whenUploadCompressedBackupAndDownloadAndDecompress_contentIsEqualToSource() {
        StorageSettings storageSettings = testUtils.buildStorageSettings(Storage.LOCAL_FILE_SYSTEM);

        try (
                InputStream backupStream = databaseBackupManager.createBackup(masterDatabaseSettings)
        ) {
            byte[] sourceBackupContent = testUtils.getStreamCopyAsByteArray(backupStream);
            try (
                    InputStream inputStream = new ByteArrayInputStream(sourceBackupContent);
                    InputStream copyInputStream = new ByteArrayInputStream(sourceBackupContent);
                    InputStream compressedBackup = backupCompressor.compressBackup(copyInputStream);
                    InputStream downloadedCompressedBackup = testUtils.uploadAndDownloadBinaryBackup(compressedBackup,
                            masterDatabaseSettings.getName(), storageSettings);
                    InputStream decompressedDownloadedBackup = backupCompressor.decompressBackup(downloadedCompressedBackup)
            ) {
                assertTrue(testUtils.streamsContentEquals(inputStream, decompressedDownloadedBackup));

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}