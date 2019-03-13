package com.example.demo;

import com.example.demo.BackupManager.PostgresBackupManager;
import com.example.demo.Storage.FileSystemTextStorage;
import com.example.demo.settings.DatabaseSettings;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {DemoApplication.class, TestConfiguration.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureEmbeddedDatabase(beanName = "masterDataSource")
@AutoConfigureEmbeddedDatabase(beanName = "copyDataSource")
public class FileSystemStoragePostgresBackupManagerTests {
    @Autowired
    PostgresBackupManager postgresBackupManager;

    @Autowired
    FileSystemTextStorage fileSystemTextStorage;

    @Autowired
    @Qualifier("jdbcMaster")
    private JdbcTemplate jdbcMasterTemplate;

    @Autowired
    @Qualifier("jdbcCopy")
    private JdbcTemplate jdbcCopyTemplate;

    @Autowired
    @Qualifier("masterDatabaseSettings")
    private DatabaseSettings masterDatabaseSettings;

    @Autowired
    @Qualifier("copyDatabaseSettings")
    private DatabaseSettings copyDatabaseSettings;

    @Test
    public void testCreateAndRestorePgBackupFromFileSystem() {
        System.out.println("Master database settings: ");
        System.out.println(masterDatabaseSettings.getUrl());
        System.out.println("Copy database settings: ");
        System.out.println(copyDatabaseSettings.getUrl());

        try {
            System.out.println(jdbcMasterTemplate.getDataSource().getConnection().getMetaData().getURL());
            System.out.println(jdbcMasterTemplate.getDataSource().getConnection().getMetaData().getUserName());
        } catch (SQLException | NullPointerException ex) {
            System.err.println("Error accessing data source");
            System.err.println(ex);
        }

        jdbcMasterTemplate.execute("CREATE TABLE comments" +
                "(" +
                "ID        SERIAL PRIMARY KEY," +
                "AUTHOR    CHARACTER VARYING(36)   not null," +
                "DATE      TIMESTAMPTZ DEFAULT NOW()," +
                "CONTENT   CHARACTER VARYING(2048) not null" +
                ")");

        long databaseRows = 10000L * 4;
        jdbcMasterTemplate.update("insert into comments (author, content)" +
                " select " +
                "    left(md5(i::text), 36)," +
                "    left(md5(random()::text), 2048) " +
                "from generate_series(0, ?) s(i)", databaseRows);


        InputStream backupStream = postgresBackupManager.createDbDump();

        BufferedReader backupStreamReader = new BufferedReader(new InputStreamReader(backupStream));
        try {
            long maxChunkSize = 1024L * 1024 * 100; // 50 Mb
            int currentChunkSize = 0;
            StringBuilder backupChunk = new StringBuilder();
            String currentLine;
            while ((currentLine = backupStreamReader.readLine()) != null) {
                backupChunk.append(currentLine);
                backupChunk.append("\n");
                currentChunkSize += currentLine.getBytes().length;
                if (currentChunkSize >= maxChunkSize) {
                    fileSystemTextStorage.saveBackup(backupChunk.toString());
                    currentChunkSize = 0;
                    backupChunk.setLength(0);
                }
            }
            if (currentChunkSize != 0) {
                fileSystemTextStorage.saveBackup(backupChunk.toString());
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        postgresBackupManager.setDatabaseSettings(copyDatabaseSettings);
        postgresBackupManager.restoreDbDump(fileSystemTextStorage.downloadBackup());

        System.out.println("Database restored");

        long startRangeId = 0;
        long rowsPerRequest = 10000;
        while (startRangeId < databaseRows) {
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
