package com.example.demo;

import com.example.demo.DbDumpHandler.PostgresDumpHandler;
import com.example.demo.StorageHandler.FileSystemTextStorageHandler;
import com.example.demo.settings.DatabaseSettings;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
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
@AutoConfigureEmbeddedDatabase(beanName = "dataSource")
public class DbDumpHandlerTests {
    @Autowired
    PostgresDumpHandler postgresDumpHandler;

    @Autowired
    FileSystemTextStorageHandler fileSystemTextStorageHandler;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DatabaseSettings databaseSettings;

    @Test
    public void testCreateAndRestorePgBackup() {
        try {
            System.out.println(jdbcTemplate.getDataSource().getConnection().getMetaData().getURL());
            System.out.println(jdbcTemplate.getDataSource().getConnection().getMetaData().getUserName());
        } catch (SQLException | NullPointerException ex) {
            System.err.println("Error accessing data source");
            System.err.println(ex);
        }

        jdbcTemplate.execute("CREATE TABLE comments" +
                "(" +
                "ID        SERIAL PRIMARY KEY," +
                "AUTHOR    CHARACTER VARYING(36)   not null," +
                "DATE      TIMESTAMPTZ DEFAULT NOW()," +
                "CONTENT   CHARACTER VARYING(2048) not null" +
                ");");

        // 1 запись - 2 кб, тогда 1073741824 - байт - 1048576 Кбайт
        for (int i = 0; i < 524288 * 2; i++) {
            jdbcTemplate.update("INSERT INTO comments(author, content) values (?, ?)",
                    RandomStringUtils.randomAlphabetic(1, 36), RandomStringUtils.randomAlphanumeric(1, 2048));
        }

        List<Map<String, Object>> oldData = jdbcTemplate.queryForList("SELECT * FROM comments");

        InputStream backupStream = postgresDumpHandler.createDbDump();

        BufferedReader backupStreamReader = new BufferedReader(new InputStreamReader(backupStream));
        try {
            StringBuilder backupChunk = new StringBuilder();
            String currentLine;
            while ((currentLine = backupStreamReader.readLine()) != null) {
                backupChunk.append(currentLine);
                backupChunk.append("\n");
            }
            fileSystemTextStorageHandler.saveBackup(backupChunk.toString());
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        jdbcTemplate.execute("DROP TABLE comments");

        System.out.println("Database dropped");

        postgresDumpHandler.restoreDbDump(fileSystemTextStorageHandler.downloadBackup());

        System.out.println("Database restored");

        List<Map<String, Object>> restoredData = jdbcTemplate.queryForList("SELECT * FROM comments");

        System.out.println("Comparing tables");

        assertEquals(oldData, restoredData);
    }
}
