package com.example.demo.StorageServiceTests;

import com.example.demo.ApplicationTests;
import com.example.demo.TestUtils;
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

import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest
public class FileSystemStorageTests extends ApplicationTests {
    private TestUtils testUtils;

    private DatabaseBackupManager databaseBackupManager;

    private JdbcTemplate jdbcPostgresMasterTemplate;

    private DatabaseSettings masterPostgresDatabaseSettings;

    private BackupProcessorManager backupProcessorManager;

    private BackupLoadManager backupLoadManager;

    private StorageSettings localFileSystemStorageSettings;

    @Autowired
    public void setTestUtils(TestUtils testUtils) {
        this.testUtils = testUtils;
    }

    @Autowired
    public void setMasterPostgresDatabaseSettings(DatabaseSettings masterPostgresDatabaseSettings) {
        this.masterPostgresDatabaseSettings = masterPostgresDatabaseSettings;
    }

    @Autowired
    public void setDatabaseBackupManager(DatabaseBackupManager databaseBackupManager) {
        this.databaseBackupManager = databaseBackupManager;
    }

    @Autowired
    public void setJdbcPostgresMasterTemplate(JdbcTemplate jdbcPostgresMasterTemplate) {
        this.jdbcPostgresMasterTemplate = jdbcPostgresMasterTemplate;
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
    public void setLocalFileSystemStorageSettings(StorageSettings localFileSystemStorageSettings) {
        this.localFileSystemStorageSettings = localFileSystemStorageSettings;
    }

    @Before
    public void setUp() {
        testUtils.clearDatabase(jdbcPostgresMasterTemplate);
        testUtils.initDatabase(jdbcPostgresMasterTemplate);
    }

    @Test
    public void whenUploadTextBackupAndDownload_contentIsEqual() {
        List<String> processors = new ArrayList<>();

        try (
                InputStream backupStream = databaseBackupManager.createBackup(masterPostgresDatabaseSettings)
        ) {
            byte[] streamContent = testUtils.getStreamCopyAsByteArray(backupStream);
            try (
                    InputStream inputStream = new ByteArrayInputStream(streamContent);
                    InputStream copyInputStream = new ByteArrayInputStream(streamContent)
            ) {
                BackupProperties backupProperties = backupLoadManager.uploadBackup(copyInputStream, localFileSystemStorageSettings,
                        processors,
                        masterPostgresDatabaseSettings.getName());
                try (
                        InputStream downloadedBackup = backupLoadManager.downloadBackup(localFileSystemStorageSettings,
                                backupProperties)
                ) {
                    assertTrue(testUtils.streamsContentEquals(inputStream, downloadedBackup));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void whenUploadCompressedBackupAndDownload_contentIsEqual() {
        List<String> processors = new ArrayList<>();
        processors.add("Processor");

        try (
                InputStream backupStream = databaseBackupManager.createBackup(masterPostgresDatabaseSettings);
                InputStream compressedBackup = backupProcessorManager.process(backupStream, processors)
        ) {
            byte[] compressedBackupContent = testUtils.getStreamCopyAsByteArray(compressedBackup);
            try (
                    InputStream inputStream = new ByteArrayInputStream(compressedBackupContent);
                    InputStream copyInputStream = new ByteArrayInputStream(compressedBackupContent)
            ) {
                BackupProperties backupProperties = backupLoadManager.uploadBackup(copyInputStream, localFileSystemStorageSettings, processors,
                        masterPostgresDatabaseSettings.getName());
                try (
                        InputStream downloadedBackup = backupLoadManager.downloadBackup(localFileSystemStorageSettings, backupProperties)
                ) {
                    assertTrue(testUtils.streamsContentEquals(inputStream, downloadedBackup));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void whenUploadCompressedBackupAndDownloadAndDecompress_contentIsEqualToSource() {
        List<String> processors = new ArrayList<>();
        processors.add("Processor");

        try (
                InputStream backupStream = databaseBackupManager.createBackup(masterPostgresDatabaseSettings)
        ) {
            byte[] sourceBackupContent = testUtils.getStreamCopyAsByteArray(backupStream);
            try (
                    InputStream inputStream = new ByteArrayInputStream(sourceBackupContent);
                    InputStream copyInputStream = new ByteArrayInputStream(sourceBackupContent);
                    InputStream compressedBackup = backupProcessorManager.process(copyInputStream, processors)
            ) {
                BackupProperties backupProperties = backupLoadManager.uploadBackup(compressedBackup, localFileSystemStorageSettings, processors,
                        masterPostgresDatabaseSettings.getName());
                try (
                        InputStream downloadedBackup = backupLoadManager.downloadBackup(localFileSystemStorageSettings, backupProperties);
                        InputStream decompressedBackup = backupProcessorManager.deprocess(downloadedBackup, processors)
                ) {
                    assertTrue(testUtils.streamsContentEquals(inputStream, decompressedBackup));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}