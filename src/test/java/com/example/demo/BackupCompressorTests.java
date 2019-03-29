package com.example.demo;


import com.example.demo.entities.database.DatabaseSettings;
import com.example.demo.manager.DatabaseBackupManager;
import com.example.demo.service.processor.BackupCompressor;
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
public class BackupCompressorTests extends ApplicationTests {
    private TestUtils testUtils;

    private BackupCompressor backupCompressor;

    private DatabaseBackupManager databaseBackupManager;

    private JdbcTemplate jdbcMasterTemplate;

    private DatabaseSettings masterDatabaseSettings;

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
    public void whenCompressAndDecompressBackup_contentIsEqualToSource() {
        testUtils.clearDatabase(jdbcMasterTemplate);
        testUtils.initDatabase(jdbcMasterTemplate);
        try (
                InputStream backupStream = databaseBackupManager.createBackup(masterDatabaseSettings)
        ) {
            byte[] streamContent = testUtils.getStreamCopyAsByteArray(backupStream);
            try (
                    InputStream inputStream = new ByteArrayInputStream(streamContent);
                    InputStream copyInputStream = new ByteArrayInputStream(streamContent);
                    InputStream compressedBackup = backupCompressor.compressBackup(inputStream);
                    InputStream decompressedBackup = backupCompressor.decompressBackup(compressedBackup)
            ) {
                assertTrue(testUtils.streamsContentEquals(copyInputStream, decompressedBackup));
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
