package com.blog;

import org.apache.commons.io.IOUtils;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.hamcrest.TypeSafeMatcher;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

public class TestUtils {
    private TestUtils() {
    }

    private static final Random random = new Random();

    /**
     * Hamcrest matcher for comparing tables content of two databases.
     * <p>
     * This matcher is not loading all table data in memory, but retrieves pages.
     * <p>
     * Tables must have INTEGER (serial) primary key 'id' column.
     * <p>
     * Example of using:
     * <pre>
     * import static com.blog.TestUtils.*;
     *
     * assertThat(slaveDatabase, equalToMasterDatabase(masterDatabase, tableNames));
     * </pre>
     *
     * @param tableNames names of tables to compare
     * @param expected   the master database
     * @return hamcrest matcher
     */
    public static Matcher<JdbcTemplate> equalToMasterDatabase(JdbcTemplate expected, List<String> tableNames) {
        return new equalsToMaster(expected, tableNames);
    }

    /**
     * Initializes database with some tables.
     * <p>
     * Use it only when you need database contain some data (e.g. for storage upload testing) but not the table itself.
     * You should not rely on realization of this function - table name, columns and other params can be changed.
     * <p>
     * Table names created by this function has '__' prefix
     *
     * @param jdbcTemplate jdbc template to perform initializing on
     */
    public static void initDatabase(JdbcTemplate jdbcTemplate) {
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
     * <p>
     * Scheme will not be deleted.
     *
     * @param jdbcTemplate jdbc template to perform cleaning on
     */
    public static void clearDatabase(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("DROP SCHEMA public CASCADE; CREATE SCHEMA public;");
    }

    /**
     * Returns stream content as byte array.
     * <p>
     * Passed input stream will not be available for reading anymore.
     *
     * @param inputStream input stream of which to make a byte array copy
     * @return content of stream as byte array
     */
    public static byte[] getStreamCopyAsByteArray(InputStream inputStream) {
        try {
            return IOUtils.toByteArray(inputStream);
        } catch (IOException ex) {
            throw new RuntimeException("Error occurred creating byte array copy of Input Stream", ex);
        }
    }

    /**
     * Generates byte array of given length with random content.
     *
     * @param length length of byte array to create
     * @return byte array filled with random bytes
     */
    public static byte[] getRandomBytes(int length) {
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);
        return bytes;
    }

    /**
     * Hamcrest matcher for comparing content of two streams.
     * <p>
     * Example of using:
     * <pre>
     * import static com.blog.TestUtils.*;
     *
     * assertThat(actualInputStream, equalToSourceInputStream(sourceInputStream));
     * </pre>
     * Both passed input stream will not be available for reading anymore.
     *
     * @param expected expected stream content
     * @return hamcrest matcher
     */
    public static Matcher<InputStream> equalToSourceInputStream(InputStream expected) {
        return new equalsToSourceStream(expected);
    }

    private static final class equalsToMaster extends TypeSafeDiagnosingMatcher<JdbcTemplate> {
        private JdbcTemplate slaveJdbcTemplate;
        private List<String> tableNames;

        equalsToMaster(JdbcTemplate slaveJdbcTemplate, List<String> tableNames) {
            this.slaveJdbcTemplate = slaveJdbcTemplate;
            this.tableNames = tableNames;
        }

        @Override
        protected boolean matchesSafely(final JdbcTemplate masterJdbcTemplate, final Description mismatchDescription) {
            long startRangeId;
            final long rowsPerQuery = 10000;

            for (String tableName : tableNames) {
                startRangeId = 0;

                Integer masterRowsAmount = Objects.requireNonNull(
                        masterJdbcTemplate.queryForObject("select COUNT(*) from " + tableName, Integer.class));
                Integer slaveRowsAmount = Objects.requireNonNull(
                        slaveJdbcTemplate.queryForObject("select COUNT(*) from " + tableName, Integer.class));

                if (!slaveRowsAmount.equals(masterRowsAmount)) {
                    mismatchDescription.appendText(
                            "Table [" + tableName + "]: slave database rows amount is not equal to master: " + slaveRowsAmount + " - " + masterJdbcTemplate);
                    return false;
                }

                int rows = masterRowsAmount;
                while (startRangeId < rows) {
                    long endRangeId = startRangeId + rowsPerQuery;
                    if (endRangeId > rows) {
                        endRangeId = rows;
                    }
                    List<Map<String, Object>> dataMaster = masterJdbcTemplate.queryForList(
                            "SELECT * FROM " + tableName + " WHERE id BETWEEN ? AND ?",
                            startRangeId, endRangeId);
                    List<Map<String, Object>> dataSlave = slaveJdbcTemplate.queryForList(
                            "SELECT * FROM " + tableName + " WHERE id BETWEEN ? AND ?",
                            startRangeId, endRangeId);
                    startRangeId = endRangeId;

                    if (!dataSlave.equals(dataMaster)) {
                        mismatchDescription.appendText(
                                "Table [" + tableName + "]: content was not equal");
                        return false;
                    }
                }
            }
            return true;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("Database tables content must be equal");
        }
    }

    private static final class equalsToSourceStream extends TypeSafeMatcher<InputStream> {
        private InputStream expected;

        equalsToSourceStream(InputStream expected) {
            this.expected = expected;
        }

        @Override
        protected boolean matchesSafely(InputStream actual) {
            try {
                return IOUtils.contentEquals(expected, actual);
            } catch (IOException ex) {
                throw new RuntimeException("Error occurred while comparing content of streams", ex);
            }
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("Streams content are not equal");
        }
    }
}
