package com.blog.controllers.WebApi;

import com.blog.ApplicationTests;
import com.blog.TestUtils;
import com.blog.entities.backup.BackupProperties;
import com.blog.entities.database.DatabaseSettings;
import com.blog.entities.database.DatabaseType;
import com.blog.entities.storage.StorageSettings;
import com.blog.entities.storage.StorageType;
import com.blog.manager.*;
import com.blog.webUI.formTransfer.WebCreateBackupRequest;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WebApiCreateBackupControllerTests extends ApplicationTests {
    private static final Integer testTaskID = 0;

    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private JdbcTemplateLockProvider jdbcTemplateLockProvider;

    @Autowired
    private TestUtils testUtils;

    @Autowired
    private JdbcTemplate jdbcPostgresMasterTemplate;

    @Autowired
    private DatabaseSettingsManager databaseSettingsManager;

    @Autowired
    private StorageSettingsManager storageSettingsManager;

    @Autowired
    private HashMap<StorageType, String> storageSettingsNameMap;

    @Autowired
    private HashMap<DatabaseType, String> databaseSettingsNameMap;

    @Autowired
    private DatabaseSettings masterPostgresDatabaseSettings;

    @Autowired
    private BackupPropertiesManager backupPropertiesManager;

    @Autowired
    private DatabaseBackupManager databaseBackupManager;

    @Autowired
    private BackupLoadManager backupLoadManager;

    @Autowired
    private WebApiClient webApiClient;

    @Autowired
    private List<DatabaseSettings> allDatabaseSettings;

    @Autowired
    private List<StorageSettings> allStorageSettings;

    @BeforeAll
    void setup() {
        databaseSettingsManager.saveAll(allDatabaseSettings);
        storageSettingsManager.saveAll(allStorageSettings);
        webApiClient.setRestTemplate(restTemplate);
        webApiClient.login();
        jdbcTemplateLockProvider.clearCache();
    }

    @BeforeEach
    void init() {
        testUtils.clearDatabase(jdbcPostgresMasterTemplate);
        testUtils.initDatabase(jdbcPostgresMasterTemplate);
    }

    @Test
    void givenProperRequestWithLocalFileSystemStorageAndPostgresDatabase_createBackup_shouldCreateBackup_whenSendRequest()
            throws IOException, InterruptedException {
        String storageSettingsName = storageSettingsNameMap.get(StorageType.LOCAL_FILE_SYSTEM);
        WebCreateBackupRequest request = webApiClient.buildCreateBackupRequest(
                databaseSettingsNameMap.get(DatabaseType.POSTGRES), storageSettingsName);

        ResponseEntity<String> resp = webApiClient.createBackup(request);

        assertEquals(HttpStatus.FOUND, resp.getStatusCode());

        webApiClient.waitForLastOperationComplete();

        Collection<BackupProperties> backupPropertiesCollection = backupPropertiesManager.findAllByOrderByIdDesc();
        BackupProperties backupProperties = backupPropertiesCollection.iterator().next();

        try (
                InputStream in = databaseBackupManager.createBackup(masterPostgresDatabaseSettings, testTaskID);
                InputStream downloadedBackup = backupLoadManager.downloadBackup(
                        backupProperties.getBackupName(), storageSettingsName, testTaskID)
        ) {
            assertTrue(testUtils.streamsContentEquals(in, downloadedBackup));
        }
    }

    @Test
    void givenProperRequestWithDropboxStorageAndPostgresDatabase_createBackup_shouldCreateBackupSuccessfully_whenSendRequest()
            throws IOException, InterruptedException {
        String storageSettingsName = storageSettingsNameMap.get(StorageType.DROPBOX);
        WebCreateBackupRequest request = webApiClient.buildCreateBackupRequest(
                databaseSettingsNameMap.get(DatabaseType.POSTGRES), storageSettingsName);
        ResponseEntity<String> resp = webApiClient.createBackup(request);

        assertEquals(HttpStatus.FOUND, resp.getStatusCode());

        webApiClient.waitForLastOperationComplete();

        Collection<BackupProperties> backupPropertiesCollection = backupPropertiesManager.findAllByOrderByIdDesc();
        BackupProperties backupProperties = backupPropertiesCollection.iterator().next();

        try (
                InputStream in = databaseBackupManager.createBackup(masterPostgresDatabaseSettings, testTaskID);
                InputStream downloadedBackup = backupLoadManager.downloadBackup(
                        backupProperties.getBackupName(), storageSettingsName, testTaskID)
        ) {
            assertTrue(testUtils.streamsContentEquals(in, downloadedBackup));
        }
    }
}
