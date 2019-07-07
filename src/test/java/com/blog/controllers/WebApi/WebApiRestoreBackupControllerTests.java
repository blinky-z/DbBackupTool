package com.blog.controllers.WebApi;

import com.blog.ApplicationTests;
import com.blog.entities.backup.BackupProperties;
import com.blog.entities.database.DatabaseSettings;
import com.blog.entities.database.DatabaseType;
import com.blog.entities.storage.StorageSettings;
import com.blog.entities.storage.StorageType;
import com.blog.manager.BackupPropertiesManager;
import com.blog.manager.DatabaseSettingsManager;
import com.blog.manager.StorageSettingsManager;
import com.blog.webUI.formTransfer.WebCreateBackupRequest;
import com.blog.webUI.formTransfer.WebRestoreBackupRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.blog.TestUtils.clearDatabase;
import static com.blog.TestUtils.equalToMasterDatabase;
import static org.hamcrest.MatcherAssert.assertThat;

class WebApiRestoreBackupControllerTests extends ApplicationTests {
    private static final java.util.List<String> tableNames = new ArrayList<>(Arrays.asList("comments"));
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private JdbcTemplate jdbcPostgresMasterTemplate;
    @Autowired
    private JdbcTemplate jdbcPostgresSlaveTemplate;
    @Autowired
    private BackupPropertiesManager backupPropertiesManager;
    @Autowired
    private WebApiClient webApiClient;
    @Autowired
    private Map<StorageType, String> storageSettingsNameMap;
    @Autowired
    private Map<DatabaseType, String> databaseSettingsNameMap;

    @Autowired
    private DatabaseSettingsManager databaseSettingsManager;

    @Autowired
    private StorageSettingsManager storageSettingsManager;

    @Autowired
    private List<DatabaseSettings> allDatabaseSettings;

    @Autowired
    private List<StorageSettings> allStorageSettings;
    @Autowired
    private Map<DatabaseType, String> slaveDatabaseSettingsNameMap;

    private void addTables(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("CREATE TABLE comments" +
                "(" +
                "ID        SERIAL PRIMARY KEY," +
                "AUTHOR    CHARACTER VARYING(36)   not null," +
                "DATE      TIMESTAMPTZ DEFAULT NOW()," +
                "CONTENT   CHARACTER VARYING(2048) not null" +
                ")");

        final long rowsToInsert = 10000L;
        jdbcTemplate.update("insert into comments (author, content)" +
                " select " +
                "    left(md5(i::text), 36)," +
                "    left(md5(random()::text), 2048) " +
                "from generate_series(0, ?) s(i)", rowsToInsert);
    }

    @BeforeEach
    void init() {
        if (initialized.compareAndSet(false, true)) {
            databaseSettingsManager.saveAll(allDatabaseSettings);
            storageSettingsManager.saveAll(allStorageSettings);
            webApiClient.setTestRestTemplate(restTemplate);
        }

        clearDatabase(jdbcPostgresMasterTemplate);
        clearDatabase(jdbcPostgresSlaveTemplate);
        addTables(jdbcPostgresMasterTemplate);
    }

    @Test
    void givenPostgresBackupLocatedOnLocalFileSystem_restoreBackup_shouldRestoreBackupSuccessfully_whenSendRequest() throws InterruptedException {
        String storageSettingsName = storageSettingsNameMap.get(StorageType.LOCAL_FILE_SYSTEM);
        {
            WebCreateBackupRequest request = webApiClient.buildCreateBackupRequest(
                    databaseSettingsNameMap.get(DatabaseType.POSTGRES), storageSettingsName);
            webApiClient.createBackup(request);

            webApiClient.waitForLatestTaskToComplete();
        }

        {
            Collection<BackupProperties> backupPropertiesCollection = backupPropertiesManager.findAllByOrderByIdDesc();
            BackupProperties backupProperties = Objects.requireNonNull(backupPropertiesCollection.iterator().next());

            WebRestoreBackupRequest request = webApiClient.buildRestoreBackupRequest(
                    backupProperties.getId(), storageSettingsName, slaveDatabaseSettingsNameMap.get(DatabaseType.POSTGRES));
            webApiClient.restoreBackup(request);

            webApiClient.waitForLatestTaskToComplete();
        }

        assertThat(jdbcPostgresMasterTemplate, equalToMasterDatabase(jdbcPostgresSlaveTemplate, tableNames));
    }

    @Test
    void givenPostgresBackupLocatedOnDropbox_restoreBackup_shouldRestoreBackupSuccessfully_whenSendRequest() throws InterruptedException {
        String storageSettingsName = storageSettingsNameMap.get(StorageType.DROPBOX);
        {
            WebCreateBackupRequest request = webApiClient.buildCreateBackupRequest(
                    databaseSettingsNameMap.get(DatabaseType.POSTGRES), storageSettingsName);
            webApiClient.createBackup(request);

            webApiClient.waitForLatestTaskToComplete();
        }

        {
            Collection<BackupProperties> backupPropertiesCollection = backupPropertiesManager.findAllByOrderByIdDesc();
            BackupProperties backupProperties = Objects.requireNonNull(backupPropertiesCollection.iterator().next());

            WebRestoreBackupRequest request = webApiClient.buildRestoreBackupRequest(
                    backupProperties.getId(), storageSettingsName, slaveDatabaseSettingsNameMap.get(DatabaseType.POSTGRES));
            webApiClient.restoreBackup(request);

            webApiClient.waitForLatestTaskToComplete();
        }

        assertThat(jdbcPostgresMasterTemplate, equalToMasterDatabase(jdbcPostgresSlaveTemplate, tableNames));
    }
}
