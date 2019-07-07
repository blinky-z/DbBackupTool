package com.blog.controllers.WebApi;

import com.blog.ApplicationTests;
import com.blog.entities.backup.BackupProperties;
import com.blog.entities.database.DatabaseSettings;
import com.blog.entities.database.DatabaseType;
import com.blog.entities.storage.StorageSettings;
import com.blog.entities.storage.StorageType;
import com.blog.manager.*;
import com.blog.webUI.formTransfer.WebCreateBackupRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.blog.TestUtils.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class WebApiCreateBackupControllerTests extends ApplicationTests {
    private static final Integer testTaskID = 0;
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private JdbcTemplate jdbcPostgresMasterTemplate;
    @Autowired
    private DatabaseSettingsManager databaseSettingsManager;
    @Autowired
    private StorageSettingsManager storageSettingsManager;
    @Autowired
    private Map<StorageType, String> storageSettingsNameMap;

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
    void givenProperRequestWithLocalFileSystemStorageAndPostgresDatabase_createBackup_shouldCreateBackup_whenSendRequest()
            throws IOException, InterruptedException {
        String storageSettingsName = storageSettingsNameMap.get(StorageType.LOCAL_FILE_SYSTEM);
        WebCreateBackupRequest request = webApiClient.buildCreateBackupRequest(
                databaseSettingsNameMap.get(DatabaseType.POSTGRES), storageSettingsName);

        ResponseEntity<String> resp = webApiClient.createBackup(request);

        assertEquals(HttpStatus.FOUND, resp.getStatusCode());

        webApiClient.waitForLatestTaskToComplete();

        Collection<BackupProperties> backupPropertiesCollection = backupPropertiesManager.findAllByOrderByIdDesc();
        BackupProperties backupProperties = backupPropertiesCollection.iterator().next();

        try (
                InputStream in = databaseBackupManager.createBackup(masterPostgresDatabaseSettings, testTaskID);
                InputStream downloadedBackup = backupLoadManager.downloadBackup(
                        backupProperties.getBackupName(), storageSettingsName, testTaskID)
        ) {
            assertThat(downloadedBackup, equalToSourceInputStream(in));
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

        webApiClient.waitForLatestTaskToComplete();

        Collection<BackupProperties> backupPropertiesCollection = backupPropertiesManager.findAllByOrderByIdDesc();
        BackupProperties backupProperties = backupPropertiesCollection.iterator().next();

        try (
                InputStream in = databaseBackupManager.createBackup(masterPostgresDatabaseSettings, testTaskID);
                InputStream downloadedBackup = backupLoadManager.downloadBackup(
                        backupProperties.getBackupName(), storageSettingsName, testTaskID)
        ) {
            assertThat(downloadedBackup, equalToSourceInputStream(in));
        }
    }
}
