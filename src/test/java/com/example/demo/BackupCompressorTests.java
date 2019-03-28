package com.example.demo;


import com.example.demo.entities.database.DatabaseSettings;
import com.example.demo.manager.DatabaseBackupManager;
import com.example.demo.service.processor.BackupCompressor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.*;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest
public class BackupCompressorTests extends ApplicationTests {
    private static final Logger logger = LoggerFactory.getLogger(BackupCompressorTests.class);

    private BackupCompressor backupCompressor;

    private DatabaseBackupManager databaseBackupManager;

    private JdbcTemplate jdbcMasterTemplate;

    private DatabaseSettings masterDatabaseSettings;

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

    @Test(expected = IllegalArgumentException.class)
    public void whenCompressNullBackupStream_throwIAE() {
        InputStream in = null;
        backupCompressor.compressBackup(in);
    }

    @Test(expected = IllegalArgumentException.class)
    public void whenDecompressNullBackupStream_throwIAE() {
        InputStream in = null;
        backupCompressor.decompressBackup(in);
    }

    @Test
    public void whenCompressAndDecompressBackup_contentIsEqualToSource() {
        TestUtils.clearDatabase(jdbcMasterTemplate);
        TestUtils.initDatabase(jdbcMasterTemplate);
        InputStream backupStream = databaseBackupManager.createBackup(masterDatabaseSettings);

        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[8 * 1024];
            int byteCount;
            while ((byteCount = backupStream.read(buffer)) != -1) {
                output.write(buffer, 0, byteCount);
            }
            byte[] source = output.toByteArray();

            InputStream inputStream = new ByteArrayInputStream(source);
            InputStream copyInputStream = new ByteArrayInputStream(source);

            InputStream compressedBackup = backupCompressor.compressBackup(inputStream);
            InputStream decompressedBackup = backupCompressor.decompressBackup(compressedBackup);

            BufferedReader sourceBackupReader = new BufferedReader(new InputStreamReader(copyInputStream));
            BufferedReader decompressedBackupReader = new BufferedReader(new InputStreamReader(decompressedBackup));

            String sourceLine;
            String decompressedLine;
            do {
                sourceLine = sourceBackupReader.readLine();
                decompressedLine = decompressedBackupReader.readLine();

                assertEquals(sourceLine, decompressedLine);

            } while (sourceLine != null);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
