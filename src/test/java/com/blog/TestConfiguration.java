package com.blog;

import com.blog.entities.database.DatabaseSettings;
import com.blog.entities.database.DatabaseType;
import com.blog.entities.database.PostgresSettings;
import com.blog.entities.storage.DropboxSettings;
import com.blog.entities.storage.LocalFileSystemSettings;
import com.blog.entities.storage.StorageSettings;
import com.blog.manager.DatabaseSettingsManager;
import com.blog.manager.StorageSettingsManager;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.FileSystemUtils;

import javax.sql.DataSource;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Configuration
public class TestConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(TestConfiguration.class);
    private static final String dropboxAccessToken = "tzFnUqsYFXAAAAAAAAAAG-irDd6KaODXHm7TlYvPwBytOxGRTJz-F0u4grmndSg3";
    @Autowired
    private StorageSettingsManager storageSettingsManager;
    @Autowired
    private DatabaseSettingsManager databaseSettingsManager;
    @Autowired
    @Qualifier("masterPostgresDataSource")
    private DataSource masterPostgresDataSource;
    @Autowired
    @Qualifier("copyPostgresDataSource")
    private DataSource copyPostgresDataSource;

    @Bean
    public DbxClientV2 dbxClient() {
        DbxRequestConfig config = DbxRequestConfig.newBuilder("testsDbxClient").build();
        return new DbxClientV2(config, dropboxAccessToken);
    }

    private DatabaseSettings buildDatabaseSettings(@NotNull DatabaseType databaseType, @NotNull String settingsName,
                                                   @NotNull DataSource dataSource) throws SQLException {
        DatabaseMetaData metadata = dataSource.getConnection().getMetaData();
        DatabaseSettings createdDatabaseSettings;
        switch (databaseType) {
            case POSTGRES: {
                String jdbcPrefix = "jdbc:";
                String connUrl = metadata.getURL().substring(jdbcPrefix.length());
                logger.info("Initializing {} database settings. Connection url: {}",
                        DatabaseType.POSTGRES, connUrl);
                PostgresSettings postgresSettings = new PostgresSettings();
                URI parsedConnUrl = URI.create(connUrl);

                createdDatabaseSettings = DatabaseSettings.postgresSettings(postgresSettings)
                        .withHost(parsedConnUrl.getHost())
                        .withPort(parsedConnUrl.getPort())
                        .withDatabaseName(parsedConnUrl.getPath().substring(parsedConnUrl.getPath().lastIndexOf("/") + 1))
                        .withLogin("postgres")
                        .withPassword("postgres")
                        .withSettingsName(settingsName)
                        .build();
                break;
            }
            default: {
                throw new RuntimeException("Can't build database settings: unknown database type");
            }
        }

        logger.info("Initializing database settings completed. Database type: {}. " +
                "Created Database settings: {}", DatabaseType.POSTGRES, createdDatabaseSettings);

        return createdDatabaseSettings;
    }

    @Bean
    public JdbcTemplate jdbcPostgresMasterTemplate() {
        return new JdbcTemplate(masterPostgresDataSource);
    }

    @Bean
    public JdbcTemplate jdbcPostgresCopyTemplate() {
        return new JdbcTemplate(copyPostgresDataSource);
    }

    @Bean
    public DatabaseSettings masterPostgresDatabaseSettings() throws SQLException {
        return Objects.requireNonNull(databaseSettingsManager.save(
                buildDatabaseSettings(DatabaseType.POSTGRES, "masterPostgresTestDatabaseSettings",
                        masterPostgresDataSource)));
    }

    @Bean
    public DatabaseSettings copyPostgresDatabaseSettings() throws SQLException {
        return Objects.requireNonNull(databaseSettingsManager.save(
                buildDatabaseSettings(DatabaseType.POSTGRES, "copyPostgresTestDatabaseSettings",
                        copyPostgresDataSource)));
    }

    @Bean
    public StorageSettings dropboxStorageSettings() {
        DropboxSettings dropboxSettings = new DropboxSettings();
        dropboxSettings.setAccessToken(dropboxAccessToken);

        return Objects.requireNonNull(storageSettingsManager.save(
                StorageSettings.dropboxSettings(dropboxSettings)
                        .withSettingsName("testDropboxStorageSettings")
                        .build()));
    }

    @Bean
    public StorageSettings localFileSystemStorageSettings() throws IOException {
        LocalFileSystemSettings localFileSystemSettings = new LocalFileSystemSettings();
        Path tempDirPath = Files.createTempDirectory("dbBackupTests");
        Runtime.getRuntime().addShutdownHook(new Thread(
                new Runnable() {

                    @Override
                    public void run() {
                        try {
                            FileSystemUtils.deleteRecursively(tempDirPath);
                        } catch (IOException e) {
                            logger.error("Error deleting temporary folder");
                        }
                    }
                }
        ));

        String tempDirPathAsString = tempDirPath.toAbsolutePath().toString();

        logger.info("Created temporary folder for File System Storage tests. Path: {}", tempDirPathAsString);
        localFileSystemSettings.setBackupPath(tempDirPathAsString);

        return Objects.requireNonNull(storageSettingsManager.save(
                StorageSettings.localFileSystemSettings(localFileSystemSettings)
                        .withSettingsName("testLocalFileSystemStorageSettings")
                        .build()));
    }

    @Bean
    public List<StorageSettings> allStorageSettings() throws IOException {
        return Arrays.asList(localFileSystemStorageSettings(), dropboxStorageSettings());
    }

    @Bean
    public List<DatabaseSettings> allDatabaseSettings() throws SQLException {
        return Arrays.asList(masterPostgresDatabaseSettings(), copyPostgresDatabaseSettings());
    }
}
