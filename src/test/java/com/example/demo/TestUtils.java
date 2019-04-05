package com.example.demo;

import org.apache.commons.io.IOUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

@Component
class TestUtils {
    void initDatabase(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("CREATE TABLE comments" +
                "(" +
                "ID        SERIAL PRIMARY KEY," +
                "AUTHOR    CHARACTER VARYING(36)   not null," +
                "DATE      TIMESTAMPTZ DEFAULT NOW()," +
                "CONTENT   CHARACTER VARYING(2048) not null" +
                ")");

        final long rowsToInsert = 1000L;
        jdbcTemplate.update("insert into comments (author, content)" +
                " select " +
                "    left(md5(i::text), 36)," +
                "    left(md5(random()::text), 2048) " +
                "from generate_series(0, ?) s(i)", rowsToInsert);
    }

    void clearDatabase(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("DROP SCHEMA public CASCADE; CREATE SCHEMA public;");
    }

    /**
     * Returns stream content as byte array.
     * Passed input stream will not be available for reading anymore
     *
     * @param inputStream input stream of which to make a byte array copy
     * @return content of stream as byte array
     */
    byte[] getStreamCopyAsByteArray(InputStream inputStream) {
        try {
            return IOUtils.toByteArray(inputStream);
        } catch (IOException ex) {
            throw new RuntimeException("Error occurred creating byte array copy of Input Stream", ex);
        }
    }

    /**
     * Compares content of two streams
     * Both passed input stream will not be available for reading anymore
     *
     * @param in1 the first stream
     * @param in2 the second stream
     * @return true if content of both stream are equal, false otherwise
     */
    boolean streamsContentEquals(InputStream in1, InputStream in2) {
        try {
            return IOUtils.contentEquals(in1, in2);
        } catch (IOException ex) {
            throw new RuntimeException("Error occurred while comparing content of streams", ex);
        }
    }
}
