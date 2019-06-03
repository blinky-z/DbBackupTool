package com.blog.controllers.WebApi;

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
import com.blog.webUI.formTransfer.WebDeleteBackupRequest;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WebApiDeleteBackupControllerTests extends ApplicationTests {
    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TestUtils testUtils;

    @Autowired
    private ControllersHttpClient controllersHttpClient;

    @Autowired
    private HashMap<StorageType, String> storageSettingsNameMap;

    @Autowired
    private HashMap<DatabaseType, String> databaseSettingsNameMap;

    @Autowired
    private JdbcTemplate jdbcPostgresMasterTemplate;

    @Autowired
    private JdbcTemplateLockProvider jdbcTemplateLockProvider;

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

    @BeforeAll
    void setup() {
        databaseSettingsManager.saveAll(allDatabaseSettings);
        storageSettingsManager.saveAll(allStorageSettings);
        controllersHttpClient.setRestTemplate(restTemplate);
        controllersHttpClient.login();
        jdbcTemplateLockProvider.clearCache();
    }

    @BeforeEach
    void init() {
        testUtils.clearDatabase(jdbcPostgresMasterTemplate);
        testUtils.initDatabase(jdbcPostgresMasterTemplate);
    }

    @Test
    void givenBackupSavedOnLocalFileSystem_deleteBackup_shouldDeleteBackup() throws InterruptedException {
        {
            WebCreateBackupRequest request = controllersHttpClient.buildCreateBackupRequest(
                    databaseSettingsNameMap.get(DatabaseType.POSTGRES), storageSettingsNameMap.get(StorageType.LOCAL_FILE_SYSTEM));
            controllersHttpClient.createBackup(request);

            controllersHttpClient.waitForLastOperationComplete();
        }

        {
            Collection<BackupProperties> backupPropertiesCollection = backupPropertiesManager.findAllByOrderByIdDesc();
            BackupProperties backupProperties = backupPropertiesCollection.iterator().next();

            WebDeleteBackupRequest request = controllersHttpClient.buildDeleteBackupRequest(backupProperties.getId());
            controllersHttpClient.deleteBackup(request);

            controllersHttpClient.waitForLastOperationComplete();

            assertFalse(backupPropertiesManager.existsById(backupProperties.getId()));
        }
    }

    @Test
    void givenBackupSavedOnDropbox_deleteBackup_shouldDeleteBackup() throws InterruptedException {
        {
            WebCreateBackupRequest request = controllersHttpClient.buildCreateBackupRequest(
                    databaseSettingsNameMap.get(DatabaseType.POSTGRES), storageSettingsNameMap.get(StorageType.DROPBOX));
            controllersHttpClient.createBackup(request);

            controllersHttpClient.waitForLastOperationComplete();
        }

        {
            Collection<BackupProperties> backupPropertiesCollection = backupPropertiesManager.findAllByOrderByIdDesc();
            BackupProperties backupProperties = backupPropertiesCollection.iterator().next();

            WebDeleteBackupRequest request = controllersHttpClient.buildDeleteBackupRequest(backupProperties.getId());
            controllersHttpClient.deleteBackup(request);

            controllersHttpClient.waitForLastOperationComplete();

            assertFalse(backupPropertiesManager.existsById(backupProperties.getId()));
        }
    }
}
