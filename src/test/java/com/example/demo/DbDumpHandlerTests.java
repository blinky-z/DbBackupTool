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

import java.io.InputStream;
import java.util.List;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureEmbeddedDatabase
public class DbDumpHandlerTests {
    @Autowired
    PostgresDumpHandler postgresDumpHandler;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    public void testCreateAndRestorePgBackup() {
        jdbcTemplate.execute("CREATE TABLE Сomments" +
                "(" +
                "ID        SERIAL PRIMARY KEY," +
                "AUTHOR    CHARACTER VARYING(36)   not null," +
                "DATE      TIMESTAMPTZ DEFAULT NOW()," +
                "CONTENT   CHARACTER VARYING(2048) not null" +
                ");");

        for (int i = 0; i < 10000; i++) {
            jdbcTemplate.update("INSERT INTO Comments(author, content) values (?, ?)",
                    RandomStringUtils.randomAlphabetic(1, 36), RandomStringUtils.randomAlphanumeric(1, 2048));
        }

        List<Map<String, Object>> oldData = jdbcTemplate.queryForList("SELECT * FROM Comments");

        InputStream backupStream = postgresDumpHandler.createDbDump();
        jdbcTemplate.execute("DROP TABLE Comments");

        postgresDumpHandler.restoreDbDump(backupStream);

        List<Map<String, Object>> restoredData = jdbcTemplate.queryForList("SELECT * FROM Comments");

        assert (oldData == restoredData);

//        assert (oldData.size() == restoredData.size());
//        for (int currentRow = 0; currentRow < oldData.size(); currentRow++) {
//            Map<String, Object> oldDataRow = oldData.get(currentRow);
//            Map<String, Object> restoredDataRow = oldData.get(currentRow);
//            if (oldDataRow != restoredDataRow) {
//
//            }
//        }
    }
}
