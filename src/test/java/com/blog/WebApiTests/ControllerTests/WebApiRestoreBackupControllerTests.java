package com.blog.WebApiTests.ControllerTests;

import com.blog.ApplicationTests;
import com.blog.TestUtils;
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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WebApiRestoreBackupControllerTests extends ApplicationTests {
    private static final java.util.List<String> tableNames = new ArrayList<>(Arrays.asList("comments"));

    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private TestUtils testUtils;
    @Autowired
    private JdbcTemplate jdbcPostgresMasterTemplate;
    @Autowired
    private JdbcTemplate jdbcPostgresCopyTemplate;
    @Autowired
    private BackupPropertiesManager backupPropertiesManager;
    @Autowired
    private ControllersHttpClient controllersHttpClient;

    @Autowired
    private HashMap<StorageType, String> storageSettingsNameMap;

    @Autowired
    private HashMap<DatabaseType, String> databaseSettingsNameMap;

    @Autowired
    private HashMap<DatabaseType, String> slaveDatabaseSettingsNameMap;

    @Autowired
    private DatabaseSettingsManager databaseSettingsManager;

    @Autowired
    private StorageSettingsManager storageSettingsManager;

    @Autowired
    private List<DatabaseSettings> allDatabaseSettings;

    @Autowired
    private List<StorageSettings> allStorageSettings;

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

    @BeforeAll
    void setup() {
        databaseSettingsManager.saveAll(allDatabaseSettings);
        storageSettingsManager.saveAll(allStorageSettings);
        controllersHttpClient.setRestTemplate(restTemplate);
        controllersHttpClient.login();
    }

    @BeforeEach
    void init() {
        testUtils.clearDatabase(jdbcPostgresMasterTemplate);
        testUtils.clearDatabase(jdbcPostgresCopyTemplate);
        addTables(jdbcPostgresMasterTemplate);
    }

    @Test
    void givenPostgresBackupLocatedOnLocalFileSystem_restoreBackup_shouldRestoreBackupSuccessfully_whenSendRequest() throws InterruptedException {
        {
            WebCreateBackupRequest request = controllersHttpClient.buildCreateBackupRequest(
                    databaseSettingsNameMap.get(DatabaseType.POSTGRES), storageSettingsNameMap.get(StorageType.LOCAL_FILE_SYSTEM));
            controllersHttpClient.createBackup(request);

            controllersHttpClient.waitForLastOperationComplete();
        }

        {
            Collection<BackupProperties> backupPropertiesCollection = backupPropertiesManager.findAllByOrderByIdDesc();
            BackupProperties backupProperties = Objects.requireNonNull(backupPropertiesCollection.iterator().next());

            WebRestoreBackupRequest request = controllersHttpClient.buildRestoreBackupRequest(
                    backupProperties.getId(), slaveDatabaseSettingsNameMap.get(DatabaseType.POSTGRES));
            controllersHttpClient.restoreBackup(request);

            controllersHttpClient.waitForLastOperationComplete();
        }

        // assert successful restoration
        testUtils.compareLargeTables(tableNames, jdbcPostgresMasterTemplate, jdbcPostgresCopyTemplate);
    }

    @Test
    void givenPostgresBackupLocatedOnDropbox_restoreBackup_shouldRestoreBackupSuccessfully_whenSendRequest() throws InterruptedException {
        {
            WebCreateBackupRequest request = controllersHttpClient.buildCreateBackupRequest(
                    databaseSettingsNameMap.get(DatabaseType.POSTGRES), storageSettingsNameMap.get(StorageType.DROPBOX));
            controllersHttpClient.createBackup(request);

            controllersHttpClient.waitForLastOperationComplete();
        }

        {
            Collection<BackupProperties> backupPropertiesCollection = backupPropertiesManager.findAllByOrderByIdDesc();
            BackupProperties backupProperties = Objects.requireNonNull(backupPropertiesCollection.iterator().next());

            WebRestoreBackupRequest request = controllersHttpClient.buildRestoreBackupRequest(
                    backupProperties.getId(), slaveDatabaseSettingsNameMap.get(DatabaseType.POSTGRES));
            controllersHttpClient.restoreBackup(request);

            controllersHttpClient.waitForLastOperationComplete();
        }

        // assert successful restoration
        testUtils.compareLargeTables(tableNames, jdbcPostgresMasterTemplate, jdbcPostgresCopyTemplate);
    }
}
