package com.blog;


import com.blog.manager.DatabaseBackupManager;
import com.blog.service.processor.BackupCompressor;
import com.blog.entities.database.DatabaseSettings;
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

    private JdbcTemplate jdbcPostgresMasterTemplate;

    private DatabaseSettings masterPostgresDatabaseSettings;

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
    public void setBackupCompressor(BackupCompressor backupCompressor) {
        this.backupCompressor = backupCompressor;
    }

    @Test
    public void whenCompressAndDecompressBackup_contentIsEqualToSource() {
        testUtils.clearDatabase(jdbcPostgresMasterTemplate);
        testUtils.initDatabase(jdbcPostgresMasterTemplate);
        try (
                InputStream backupStream = databaseBackupManager.createBackup(masterPostgresDatabaseSettings)
        ) {
            byte[] streamContent = testUtils.getStreamCopyAsByteArray(backupStream);
            try (
                    InputStream inputStream = new ByteArrayInputStream(streamContent);
                    InputStream copyInputStream = new ByteArrayInputStream(streamContent);
                    InputStream compressedBackup = backupCompressor.process(inputStream);
                    InputStream decompressedBackup = backupCompressor.deprocess(compressedBackup)
            ) {
                assertTrue(testUtils.streamsContentEquals(copyInputStream, decompressedBackup));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
