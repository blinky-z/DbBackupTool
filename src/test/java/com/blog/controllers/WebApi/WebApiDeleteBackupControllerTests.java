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
import com.blog.webUI.formTransfer.WebDeleteBackupRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.blog.TestUtils.clearDatabase;
import static com.blog.TestUtils.initDatabase;
import static org.junit.jupiter.api.Assertions.assertFalse;

class WebApiDeleteBackupControllerTests extends ApplicationTests {
    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private WebApiClient webApiClient;

    private final AtomicBoolean initialized = new AtomicBoolean(false);
    @Autowired
    private Map<StorageType, String> storageSettingsNameMap;

    @Autowired
    private JdbcTemplate jdbcPostgresMasterTemplate;

    @Autowired
    private BackupPropertiesManager backupPropertiesManager;

    @Autowired
    private DatabaseSettingsManager databaseSettingsManager;

    @Autowired
    private StorageSettingsManager storageSettingsManager;

    @Autowired
    private List<DatabaseSettings> allDatabaseSettings;

    @Autowired
    private List<StorageSettings> allStorageSettings;
    @Autowired
    private Map<DatabaseType, String> databaseSettingsNameMap;

    @BeforeEach
    void init() {
        if (initialized.compareAndSet(false, true)) {
            databaseSettingsManager.saveAll(allDatabaseSettings);
            storageSettingsManager.saveAll(allStorageSettings);
            webApiClient.setTestRestTemplate(restTemplate);
        }

        clearDatabase(jdbcPostgresMasterTemplate);
        initDatabase(jdbcPostgresMasterTemplate);
    }

    @Test
    void givenBackupSavedOnLocalFileSystem_deleteBackup_shouldDeleteBackup() throws InterruptedException {
        {
            WebCreateBackupRequest request = webApiClient.buildCreateBackupRequest(
                    databaseSettingsNameMap.get(DatabaseType.POSTGRES), storageSettingsNameMap.get(StorageType.LOCAL_FILE_SYSTEM));
            webApiClient.createBackup(request);

            webApiClient.waitForLatestTaskToComplete();
        }

        {
            Collection<BackupProperties> backupPropertiesCollection = backupPropertiesManager.findAllByOrderByIdDesc();
            BackupProperties backupProperties = backupPropertiesCollection.iterator().next();

            WebDeleteBackupRequest request = webApiClient.buildDeleteBackupRequest(backupProperties.getId());
            webApiClient.deleteBackup(request);

            webApiClient.waitForLatestTaskToComplete();

            assertFalse(backupPropertiesManager.existsById(backupProperties.getId()));
        }
    }

    @Test
    void givenBackupSavedOnDropbox_deleteBackup_shouldDeleteBackup() throws InterruptedException {
        {
            WebCreateBackupRequest request = webApiClient.buildCreateBackupRequest(
                    databaseSettingsNameMap.get(DatabaseType.POSTGRES), storageSettingsNameMap.get(StorageType.DROPBOX));
            webApiClient.createBackup(request);

            webApiClient.waitForLatestTaskToComplete();
        }

        {
            Collection<BackupProperties> backupPropertiesCollection = backupPropertiesManager.findAllByOrderByIdDesc();
            BackupProperties backupProperties = backupPropertiesCollection.iterator().next();

            WebDeleteBackupRequest request = webApiClient.buildDeleteBackupRequest(backupProperties.getId());
            webApiClient.deleteBackup(request);

            webApiClient.waitForLatestTaskToComplete();

            assertFalse(backupPropertiesManager.existsById(backupProperties.getId()));
        }
    }
}
