package com.example.demo;

import com.example.demo.DbDumpHandler.PostgresDumpHandler;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import javax.sql.DataSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.sql.SQLException;
import java.sql.Time;
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
    private DataSource dataSource;

    @Test
    public void testCreateAndRestorePgBackup() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        try {
            System.out.println(dataSource.getConnection().getMetaData().getURL());
            System.out.println(dataSource.getConnection().getMetaData().getUserName());
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

        for (int i = 0; i < 100; i++) {
            jdbcTemplate.update("INSERT INTO comments(author, content) values (?, ?)",
                    RandomStringUtils.randomAlphabetic(1, 36), RandomStringUtils.randomAlphanumeric(1, 2048));
        }

        List<Map<String, Object>> oldData = jdbcTemplate.queryForList("SELECT * FROM comments");

        InputStream backupStream = postgresDumpHandler.createDbDump();
        jdbcTemplate.execute("DROP TABLE comments");

        System.out.println("Comments table dropped");

//        File createCommentsTableFile = new File("C:\\Users\\User\\IdeaProjects\\DatabaseBackupTool\\test_create_comments.dump");
//        try {
//            backupStream = new FileInputStream(createCommentsTableFile);
//        } catch (FileNotFoundException ex) {
//            ex.printStackTrace();
//        }

        postgresDumpHandler.restoreDbDump(backupStream);

        System.out.println("Comments table restored");

        List<Map<String, Object>> restoredData = jdbcTemplate.queryForList("SELECT * FROM comments");

        System.out.println("Comparing tables");

        assertEquals(oldData, restoredData);
    }
}
