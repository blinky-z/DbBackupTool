package com.example.demo;

import com.example.demo.entities.database.DatabaseSettings;
import com.example.demo.entities.storage.Storage;
import com.example.demo.entities.storage.StorageSettings;
import com.example.demo.manager.DatabaseBackupManager;
import com.example.demo.manager.TextStorageBackupLoadManager;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import javax.sql.DataSource;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {DemoApplication.class, TestConfiguration.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureEmbeddedDatabase(beanName = "masterDataSource")
@AutoConfigureEmbeddedDatabase(beanName = "copyDataSource")
@TestPropertySource(locations = "classpath:tests.properties")
public class PostgresDatabaseTests {
    private JdbcTemplate jdbcMasterTemplate;

    private JdbcTemplate jdbcCopyTemplate;

    private DatabaseSettings masterDatabaseSettings;

    private DatabaseSettings copyDatabaseSettings;

    private DatabaseBackupManager databaseBackupManager;

    private TextStorageBackupLoadManager textStorageBackupLoadManager;

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
    public void setTextStorageBackupLoadManager(TextStorageBackupLoadManager textStorageBackupLoadManager) {
        this.textStorageBackupLoadManager = textStorageBackupLoadManager;
    }

    @Before
    public void clearDatabase() {
        jdbcMasterTemplate.execute("DROP SCHEMA public CASCADE; CREATE SCHEMA public;");
        jdbcCopyTemplate.execute("DROP SCHEMA public CASCADE; CREATE SCHEMA public;");
    }

    @Test
    public void createPostgresBackupAndUploadToLocalFileSystemAndRestore() {
        StorageSettings storageSettings = TestUtils.buildStorageSettings(Storage.LOCAL_FILE_SYSTEM);

        jdbcMasterTemplate.execute("CREATE TABLE comments" +
                "(" +
                "ID        SERIAL PRIMARY KEY," +
                "AUTHOR    CHARACTER VARYING(36)   not null," +
                "DATE      TIMESTAMPTZ DEFAULT NOW()," +
                "CONTENT   CHARACTER VARYING(2048) not null" +
                ")");

        long rowsToInsert = 1000L;
        jdbcMasterTemplate.update("insert into comments (author, content)" +
                " select " +
                "    left(md5(i::text), 36)," +
                "    left(md5(random()::text), 2048) " +
                "from generate_series(0, ?) s(i)", rowsToInsert);

        InputStream createdBackup = databaseBackupManager.createBackup(masterDatabaseSettings);

        InputStream backupFromStorage = TestUtils.uploadAndDownloadBackup(createdBackup, masterDatabaseSettings.getName(),
                storageSettings);

        databaseBackupManager.restoreBackup(backupFromStorage, copyDatabaseSettings);

        long startRangeId = 0;
        long rowsPerRequest = 10000;
        while (startRangeId < rowsToInsert) {
            long endRangeId = startRangeId + rowsPerRequest;
            List<Map<String, Object>> oldData = jdbcMasterTemplate.queryForList("SELECT * FROM comments WHERE id BETWEEN ? AND ?",
                    startRangeId, endRangeId);
            List<Map<String, Object>> restoredData = jdbcCopyTemplate.queryForList("SELECT * FROM comments WHERE id BETWEEN ? AND ?",
                    startRangeId, endRangeId);
            startRangeId = endRangeId;

            assertEquals(oldData, restoredData);
        }
    }

    @Test
    public void CreatePostgresBackupAndUploadToDropboxAndRestore() {
        StorageSettings storageSettings = TestUtils.buildStorageSettings(Storage.DROPBOX);

        jdbcMasterTemplate.execute("CREATE TABLE comments" +
                "(" +
                "ID        SERIAL PRIMARY KEY," +
                "AUTHOR    CHARACTER VARYING(36)   not null," +
                "DATE      TIMESTAMPTZ DEFAULT NOW()," +
                "CONTENT   CHARACTER VARYING(2048) not null" +
                ")");

        long rowsToInsert = 1000L;
        jdbcMasterTemplate.update("insert into comments (author, content)" +
                " select " +
                "    left(md5(i::text), 36)," +
                "    left(md5(random()::text), 2048) " +
                "from generate_series(0, ?) s(i)", rowsToInsert);

        InputStream createdBackup = databaseBackupManager.createBackup(masterDatabaseSettings);

        InputStream backupFromStorage = TestUtils.uploadAndDownloadBackup(createdBackup, masterDatabaseSettings.getName(),
                storageSettings);

        databaseBackupManager.restoreBackup(backupFromStorage, copyDatabaseSettings);

        long startRangeId = 0;
        long rowsPerRequest = 10000;
        while (startRangeId < rowsToInsert) {
            long endRangeId = startRangeId + rowsPerRequest;
            List<Map<String, Object>> oldData = jdbcMasterTemplate.queryForList("SELECT * FROM comments WHERE id BETWEEN ? AND ?",
                    startRangeId, endRangeId);
            List<Map<String, Object>> restoredData = jdbcCopyTemplate.queryForList("SELECT * FROM comments WHERE id BETWEEN ? AND ?",
                    startRangeId, endRangeId);
            startRangeId = endRangeId;

            assertEquals(oldData, restoredData);
        }
    }
}
