package com.example.demo;

import com.example.demo.entities.backup.BackupProperties;
import com.example.demo.entities.database.Database;
import com.example.demo.entities.database.DatabaseSettings;
import com.example.demo.entities.database.PostgresSettings;
import com.example.demo.entities.storage.DropboxSettings;
import com.example.demo.entities.storage.LocalFileSystemSettings;
import com.example.demo.entities.storage.Storage;
import com.example.demo.entities.storage.StorageSettings;
import com.example.demo.manager.TextStorageBackupLoadManager;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import javax.validation.constraints.NotEmpty;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.FileSystems;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Service
class TestUtils {
    private static String dropboxAccessToken;

    private static final String backup_path = FileSystems.getDefault().getPath("src/test").toAbsolutePath().toString();

    private static TextStorageBackupLoadManager textStorageBackupLoadManager;

    private static StorageSettings localFileSystemStorageSettings;

    private static StorageSettings dropboxStorageSettings;

    private static Logger logger = LoggerFactory.getLogger(TestUtils.class);

    static void initDatabase(JdbcTemplate jdbcTemplate) {
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

    static void clearDatabase(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("DROP SCHEMA public CASCADE; CREATE SCHEMA public;");
    }

    @Autowired
    public void setDropboxAccessToken(@Value("${tests-config.dropbox-access-token}") @NotEmpty String dropboxAccessToken) {
        TestUtils.dropboxAccessToken = dropboxAccessToken;
    }

    @Autowired
    public void setTextStorageBackupLoadManager(TextStorageBackupLoadManager textStorageBackupLoadManager) {
        TestUtils.textStorageBackupLoadManager = textStorageBackupLoadManager;
    }

    static DatabaseSettings buildDatabaseSettings(@NotNull Database databaseType, @NotNull DataSource dataSource) {
        try {
            DatabaseMetaData metadata = dataSource.getConnection().getMetaData();
            switch (databaseType) {
                case POSTGRES: {
                    String jdbcPrefix = "jdbc:";
                    String connUrl = metadata.getURL().substring(jdbcPrefix.length());
                    logger.info("Initializing {} database settings. Connection url: {}",
                            Database.POSTGRES, connUrl);
                    PostgresSettings postgresSettings = new PostgresSettings();
                    URI parsedConnUrl = URI.create(connUrl);

                    DatabaseSettings postgresDatabaseSettings = DatabaseSettings.postgresSettings(postgresSettings)
                            .withHost(parsedConnUrl.getHost())
                            .withPort(String.valueOf(parsedConnUrl.getPort()))
                            .withName(parsedConnUrl.getPath().substring(parsedConnUrl.getPath().lastIndexOf("/") + 1))
                            .withLogin("postgres")
                            .withPassword("postgres")
                            .build();

                    logger.info("Initializing {} database settings completed. " +
                            "Created Database settings: {}", Database.POSTGRES, postgresDatabaseSettings);
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

    static StorageSettings buildStorageSettings(@NotNull Storage storageType) {
        switch (storageType) {
            case LOCAL_FILE_SYSTEM: {
                if (localFileSystemStorageSettings == null) {
                    LocalFileSystemSettings localFileSystemSettings = new LocalFileSystemSettings();
                    localFileSystemSettings.setBackupPath(backup_path);
                    logger.info("Initializing {} settings for the first time. Configuration Details: {}",
                            Storage.LOCAL_FILE_SYSTEM, localFileSystemSettings);
                    localFileSystemStorageSettings = StorageSettings.localFileSystemSettings(localFileSystemSettings).build();

                    logger.info("Initializing {} storage settings for the first time completed. " +
                            "Storage settings: {}", Storage.LOCAL_FILE_SYSTEM, localFileSystemStorageSettings);
                }
                return localFileSystemStorageSettings;
            }
            case DROPBOX: {
                if (dropboxStorageSettings == null) {
                    DropboxSettings dropboxSettings = new DropboxSettings();
                    dropboxSettings.setAccessToken(dropboxAccessToken);
                    logger.info("Initializing {} settings for the first time. Configuration Details: {}",
                            Storage.LOCAL_FILE_SYSTEM, dropboxSettings);
                    dropboxStorageSettings = StorageSettings.dropboxSettings(dropboxSettings).build();

                    logger.info("Initializing {} storage settings for the first time completed." +
                            "{} storage settings: {}", Storage.LOCAL_FILE_SYSTEM, dropboxStorageSettings);
                }
                return dropboxStorageSettings;
            }
            default: {
                throw new RuntimeException("Can't build storage settings: unknown storage type");
            }
        }
    }

    static InputStream uploadAndDownloadTextBackup(InputStream backupStream, String databaseName,
                                                   StorageSettings storageSettings) {
        List<StorageSettings> storageSettingsList = new ArrayList<>();
        storageSettingsList.add(storageSettings);

        int maxChinkSize = 32000;
        List<BackupProperties> backupPropertiesList = textStorageBackupLoadManager.uploadBackup(backupStream,
                storageSettingsList, databaseName, maxChinkSize);
        BackupProperties backupProperties = backupPropertiesList.get(0);

        return textStorageBackupLoadManager.
                downloadBackup(storageSettings, backupProperties);
    }
}
