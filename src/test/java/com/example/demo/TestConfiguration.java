package com.example.demo;

import com.example.demo.entities.database.DatabaseSettings;
import com.example.demo.entities.database.DatabaseType;
import com.example.demo.entities.database.PostgresSettings;
import com.example.demo.entities.storage.DropboxSettings;
import com.example.demo.entities.storage.LocalFileSystemSettings;
import com.example.demo.entities.storage.StorageSettings;
import com.example.demo.repositories.DatabaseSettingsRepository;
import com.example.demo.repositories.StorageSettingsRepository;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.net.URI;
import java.nio.file.FileSystems;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Objects;

@Configuration
public class TestConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(TestConfiguration.class);
    private static final String dropboxAccessToken = "tzFnUqsYFXAAAAAAAAAAG-irDd6KaODXHm7TlYvPwBytOxGRTJz-F0u4grmndSg3";
    private static final String localFileSystemBackupPath =
            FileSystems.getDefault().getPath("src/test").toAbsolutePath().toString();
    @Autowired
    private StorageSettingsRepository storageSettingsRepository;
    @Autowired
    private DatabaseSettingsRepository databaseSettingsRepository;
    @Autowired
    @Qualifier("masterPostgresDataSource")
    private DataSource masterPostgresDataSource;
    @Autowired
    @Qualifier("copyPostgresDataSource")
    private DataSource copyPostgresDataSource;

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

                DatabaseSettings postgresDatabaseSettings = DatabaseSettings.postgresSettings(postgresSettings)
                        .withHost(parsedConnUrl.getHost())
                        .withPort(parsedConnUrl.getPort())
                        .withDatabaseName(parsedConnUrl.getPath().substring(parsedConnUrl.getPath().lastIndexOf("/") + 1))
                        .withLogin("postgres")
                        .withPassword("postgres")
                        .withSettingsName(settingsName)
                        .build();

                createdDatabaseSettings = databaseSettingsRepository.save(postgresDatabaseSettings);
                break;
            }
            default: {
                throw new RuntimeException("Can't build database settings: unknown database type");
            }
        }

        logger.info("Initializing database settings completed. Database type: {}. " +
                "Created Database settings: {}", DatabaseType.POSTGRES, createdDatabaseSettings);

        return Objects.requireNonNull(createdDatabaseSettings);
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
        return buildDatabaseSettings(DatabaseType.POSTGRES, "masterPostgresTestDatabaseSettings",
                masterPostgresDataSource);
    }

    @Bean
    public DatabaseSettings copyPostgresDatabaseSettings() throws SQLException {
        return buildDatabaseSettings(DatabaseType.POSTGRES, "copyPostgresTestDatabaseSettings",
                copyPostgresDataSource);
    }

    @Bean
    public StorageSettings dropboxStorageSettings() {
        DropboxSettings dropboxSettings = new DropboxSettings();
        dropboxSettings.setAccessToken(dropboxAccessToken);
        return Objects.requireNonNull(storageSettingsRepository.save(StorageSettings.dropboxSettings(dropboxSettings)
                .withSettingsName("testDropboxStorageSettings")
                .build()));
    }

    @Bean
    public StorageSettings localFileSystemStorageSettings() {
        LocalFileSystemSettings localFileSystemSettings = new LocalFileSystemSettings();
        localFileSystemSettings.setBackupPath(localFileSystemBackupPath);
        return Objects.requireNonNull(
                storageSettingsRepository.save(StorageSettings.localFileSystemSettings(localFileSystemSettings)
                        .withSettingsName("testLocalFileSystemStorageSettings")
                        .build()));
    }
}
