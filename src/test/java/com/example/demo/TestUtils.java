package com.example.demo;

import com.example.demo.entities.database.DatabaseType;
import com.example.demo.entities.database.DatabaseSettings;
import com.example.demo.entities.database.PostgresSettings;
import com.example.demo.entities.storage.DropboxSettings;
import com.example.demo.entities.storage.LocalFileSystemSettings;
import com.example.demo.entities.storage.StorageSettings;
import com.example.demo.entities.storage.StorageType;
import com.example.demo.manager.BackupLoadManager;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.FileSystems;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

@Component
class TestUtils {
    private static final String dropboxAccessToken = "tzFnUqsYFXAAAAAAAAAAG-irDd6KaODXHm7TlYvPwBytOxGRTJz-F0u4grmndSg3";

    private static final String backup_path = FileSystems.getDefault().getPath("src/test").toAbsolutePath().toString();

    private BackupLoadManager backupLoadManager;

    private StorageSettings localFileSystemStorageSettings;

    private StorageSettings dropboxStorageSettings;

    private Logger logger = LoggerFactory.getLogger(TestUtils.class);

    public void initDatabase(JdbcTemplate jdbcTemplate) {
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

    public void clearDatabase(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("DROP SCHEMA public CASCADE; CREATE SCHEMA public;");
    }

    @Autowired
    public void setBackupLoadManager(BackupLoadManager backupLoadManager) {
        this.backupLoadManager = backupLoadManager;
    }

    public DatabaseSettings buildDatabaseSettings(@NotNull DatabaseType databaseType, @NotNull DataSource dataSource) {
        try {
            DatabaseMetaData metadata = dataSource.getConnection().getMetaData();
            switch (databaseType) {
                case POSTGRES: {
                    String jdbcPrefix = "jdbc:";
                    String connUrl = metadata.getURL().substring(jdbcPrefix.length());
                    logger.info("Initializing {} database settings. Connection url: {}",
                            DatabaseType.POSTGRES, connUrl);
                    PostgresSettings postgresSettings = new PostgresSettings();
                    URI parsedConnUrl = URI.create(connUrl);

                    DatabaseSettings postgresDatabaseSettings = DatabaseSettings.postgresSettings(postgresSettings)
                            .withHost(parsedConnUrl.getHost())
                            .withPort(parsedConnUrl.getPort())
                            .withName(parsedConnUrl.getPath().substring(parsedConnUrl.getPath().lastIndexOf("/") + 1))
                            .withLogin("postgres")
                            .withPassword("postgres")
                            .build();

                    logger.info("Initializing {} database settings completed. " +
                            "Created Database settings: {}", DatabaseType.POSTGRES, postgresDatabaseSettings);
                    return postgresDatabaseSettings;
                }
                default: {
                    throw new RuntimeException("Can't build database settings: unknown database type");
                }
            }
        } catch (SQLException ex) {
            throw new RuntimeException("Error building database settings: database access error", ex);
        }
    }

    public StorageSettings buildStorageSettings(@NotNull StorageType storageType) {
        switch (storageType) {
            case LOCAL_FILE_SYSTEM: {
                if (localFileSystemStorageSettings == null) {
                    LocalFileSystemSettings localFileSystemSettings = new LocalFileSystemSettings();
                    localFileSystemSettings.setBackupPath(backup_path);
                    logger.info("Initializing {} settings for the first time. Configuration Details: {}",
                            StorageType.LOCAL_FILE_SYSTEM, localFileSystemSettings);
                    localFileSystemStorageSettings = StorageSettings.localFileSystemSettings(localFileSystemSettings).build();

                    logger.info("Initializing {} storage settings for the first time completed. " +
                            "StorageType settings: {}", StorageType.LOCAL_FILE_SYSTEM, localFileSystemStorageSettings);
                }
                return localFileSystemStorageSettings;
            }
            case DROPBOX: {
                if (dropboxStorageSettings == null) {
                    DropboxSettings dropboxSettings = new DropboxSettings();
                    dropboxSettings.setAccessToken(dropboxAccessToken);
                    logger.info("Initializing {} settings for the first time. Configuration Details: {}",
                            StorageType.LOCAL_FILE_SYSTEM, dropboxSettings);
                    dropboxStorageSettings = StorageSettings.dropboxSettings(dropboxSettings).build();

                    logger.info("Initializing {} storage settings for the first time completed." +
                            "storage settings: {}", StorageType.DROPBOX, dropboxStorageSettings);
                }
                return dropboxStorageSettings;
            }
            default: {
                throw new RuntimeException("Can't build storage settings: unknown storage type");
            }
        }
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
    public boolean streamsContentEquals(InputStream in1, InputStream in2) {
        try {
            return IOUtils.contentEquals(in1, in2);
        } catch (IOException ex) {
            throw new RuntimeException("Error occurred while comparing content of streams", ex);
        }
    }
}
