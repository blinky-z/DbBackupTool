package com.blog;

import org.apache.commons.io.IOUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Component
public class TestUtils {
    private static final Random random = new Random();

    /**
     * Compares tables not loading all table data in memory.
     * Table must contain 'id' column.
     *
     * @param tableNames         table names to compare
     * @param jdbcMasterTemplate the master database
     * @param jdbcCopyTemplate   the slave database
     */
    public void compareLargeTables(List<String> tableNames, JdbcTemplate jdbcMasterTemplate, JdbcTemplate jdbcCopyTemplate) {
        long startRangeId;
        final long rowsPerQuery = 10000;

        for (String tableName : tableNames) {
            startRangeId = 0;

            Integer masterRowsAmount = Objects.requireNonNull(
                    jdbcMasterTemplate.queryForObject("select COUNT(*) from " + tableName, Integer.class));
            Integer copyRowsAmount = Objects.requireNonNull(
                    jdbcCopyTemplate.queryForObject("select COUNT(*) from " + tableName, Integer.class));

            assertEquals(masterRowsAmount, copyRowsAmount);

            int rows = masterRowsAmount.intValue();
            while (startRangeId < rows) {
                long endRangeId = startRangeId + rowsPerQuery;
                if (endRangeId > rows) {
                    endRangeId = rows;
                }
                List<Map<String, Object>> dataMaster = jdbcMasterTemplate.queryForList(
                        "SELECT * FROM " + tableName + " WHERE id BETWEEN ? AND ?",
                        startRangeId, endRangeId);
                List<Map<String, Object>> dataCopy = jdbcCopyTemplate.queryForList(
                        "SELECT * FROM " + tableName + " WHERE id BETWEEN ? AND ?",
                        startRangeId, endRangeId);
                startRangeId = endRangeId;

                assertEquals(dataMaster, dataCopy);
            }
        }
    }

    /**
     * Initializes database with some tables.
     * Use it only when you need database contain some data (e.g. for storage upload testing) but not the table itself.
     * You should not rely on realization of this function - table name, columns and other params can be changed.
     * <p>
     * Table names created by this function has '__' prefix
     *
     * @param jdbcTemplate jdbc template to perform initializing on
     */
    public void initDatabase(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("CREATE TABLE __comments" +
                "(" +
                "ID        SERIAL PRIMARY KEY," +
                "AUTHOR    CHARACTER VARYING(36)   not null," +
                "DATE      TIMESTAMPTZ DEFAULT NOW()," +
                "CONTENT   CHARACTER VARYING(2048) not null" +
                ")");

        final long rowsToInsert = 10000L;
        jdbcTemplate.update("insert into __comments (author, content)" +
                " select " +
                "    left(md5(i::text), 36)," +
                "    left(md5(random()::text), 2048) " +
                "from generate_series(0, ?) s(i)", rowsToInsert);
    }

    /**
     * Drops all tables in 'public' scheme.
     * Scheme will not be deleted.
     *
     * @param jdbcTemplate jdbc template to perform cleaning on
     */
    public void clearDatabase(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("DROP SCHEMA public CASCADE; CREATE SCHEMA public;");
    }

    /**
     * Returns stream content as byte array.
     * Passed input stream will not be available for reading anymore
     *
     * @param inputStream input stream of which to make a byte array copy
     * @return content of stream as byte array
     */
    public byte[] getStreamCopyAsByteArray(InputStream inputStream) {
        try {
            return IOUtils.toByteArray(inputStream);
        } catch (IOException ex) {
            throw new RuntimeException("Error occurred creating byte array copy of Input Stream", ex);
        }
    }


    /**
     * Generates byte array with random content
     *
     * @param length length of byte array to create
     * @return byte array filled with random bytes
     */
    public byte[] getRandomBytes(int length) {
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);
        return bytes;
    }

    /**
     * Compares content of two streams
     * Both passed input stream will not be available for reading anymore
     *
     * @param in1 the first stream
     * @param in2 the second stream
     * @return true if content of both streams are equal, false otherwise
     */
    public boolean streamsContentEquals(InputStream in1, InputStream in2) {
        try {
            return IOUtils.contentEquals(in1, in2);
        } catch (IOException ex) {
            throw new RuntimeException("Error occurred while comparing content of streams", ex);
        }
    }
}
