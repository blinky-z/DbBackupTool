package com.blog.WebApiTests.ControllerTests;

import com.blog.ApplicationTests;
import com.blog.TestUtils;
import com.blog.entities.backup.BackupProperties;
import com.blog.entities.backup.BackupTask;
import com.blog.entities.database.DatabaseSettings;
import com.blog.manager.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class WebApiCreateBackupControllerTests extends ApplicationTests {
    private static final Integer testTaskID = 0;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TestUtils testUtils;

    @Autowired
    private JdbcTemplate jdbcPostgresMasterTemplate;

    @Autowired
    private DatabaseSettings masterPostgresDatabaseSettings;

    @Autowired
    private BackupPropertiesManager backupPropertiesManager;

    @Autowired
    private MultiValueMap<String, Object> masterPostgresDatabaseSettingsAsMultiValueMap;

    @Autowired
    private MultiValueMap<String, Object> localFileSystemStorageSettingsAsMultiValueMap;

    @Autowired
    private MultiValueMap<String, Object> dropboxStorageSettingsAsMultiValueMap;

    @Autowired
    private StorageSettingsManager storageSettingsManager;

    @Autowired
    private DatabaseSettingsManager databaseSettingsManager;

    @Autowired
    private DatabaseBackupManager databaseBackupManager;

    @Autowired
    private BackupLoadManager backupLoadManager;

    @Autowired
    private BackupTaskManager backupTaskManager;

    @Before
    public void init() {
        testUtils.clearDatabase(jdbcPostgresMasterTemplate);
        testUtils.initDatabase(jdbcPostgresMasterTemplate);
    }

    @Test
    public void givenProperRequestWithLocalFileSystemStorage_createBackup_shouldCreateBackupSuccessfully_whenSendRequest() throws IOException, InterruptedException {
        String settingsName =
                "givenProperRequestWithLocalFileSystemStorage_createBackup_shouldCreateBackupSuccessfully_whenSendRequest";

        {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>(localFileSystemStorageSettingsAsMultiValueMap);
            body.add("settingsName", settingsName);

            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> responseEntity = restTemplate.exchange("/storage", HttpMethod.POST, entity, String.class);

            assertEquals(HttpStatus.FOUND, responseEntity.getStatusCode());
        }

        {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>(masterPostgresDatabaseSettingsAsMultiValueMap);
            body.add("settingsName", settingsName);

            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> responseEntity = restTemplate.exchange("/database", HttpMethod.POST, entity, String.class);

            assertEquals(HttpStatus.FOUND, responseEntity.getStatusCode());
        }

        {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("databaseSettingsName", settingsName);
            body.add("backupCreationProperties[" + settingsName + "].storageSettingsName", settingsName);

            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> responseEntity = restTemplate.exchange(
                    "/create-backup", HttpMethod.POST, entity, String.class);

            assertEquals(HttpStatus.FOUND, responseEntity.getStatusCode());

            BackupTask backupTask = backupTaskManager.findAllByOrderByDateDesc().iterator().next();
            Integer id = backupTask.getId();

            while (backupTaskManager.getBackupTask(id).orElseThrow(RuntimeException::new).getState() != BackupTask.State.COMPLETED) {
                Thread.sleep(300);
            }

            Collection<BackupProperties> backupPropertiesCollection = backupPropertiesManager.findAllByOrderByIdDesc();
            BackupProperties backupProperties = backupPropertiesCollection.iterator().next();

            try (
                    InputStream in = databaseBackupManager.createBackup(masterPostgresDatabaseSettings, 0);
                    InputStream downloadedBackup = backupLoadManager.downloadBackup(backupProperties, testTaskID)
            ) {
                assertTrue(testUtils.streamsContentEquals(in, downloadedBackup));
            }
        }
    }

    @Test
    public void givenProperRequestWithDropboxStorage_createBackup_shouldCreateBackupSuccessfully_whenSendRequest() throws IOException, InterruptedException {
        String settingsName =
                "givenProperRequestWithDropboxStorage_createBackup_shouldCreateBackupSuccessfully_whenSendRequest";

        {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>(dropboxStorageSettingsAsMultiValueMap);
            body.add("settingsName", settingsName);

            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> responseEntity = restTemplate.exchange("/storage", HttpMethod.POST, entity, String.class);

            assertEquals(HttpStatus.FOUND, responseEntity.getStatusCode());
        }

        {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>(masterPostgresDatabaseSettingsAsMultiValueMap);
            body.add("settingsName", settingsName);

            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> responseEntity = restTemplate.exchange("/database", HttpMethod.POST, entity, String.class);

            assertEquals(HttpStatus.FOUND, responseEntity.getStatusCode());
        }

        {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("databaseSettingsName", settingsName);
            body.add("backupCreationProperties[" + settingsName + "].storageSettingsName", settingsName);

            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> responseEntity = restTemplate.exchange(
                    "/create-backup", HttpMethod.POST, entity, String.class);

            assertEquals(HttpStatus.FOUND, responseEntity.getStatusCode());

            BackupTask backupTask = backupTaskManager.findAllByOrderByDateDesc().iterator().next();
            Integer id = backupTask.getId();

            while (backupTaskManager.getBackupTask(id).orElseThrow(RuntimeException::new).getState() != BackupTask.State.COMPLETED) {
                Thread.sleep(300);
            }

            Collection<BackupProperties> backupPropertiesCollection = backupPropertiesManager.findAllByOrderByIdDesc();
            BackupProperties backupProperties = backupPropertiesCollection.iterator().next();

            try (
                    InputStream in = databaseBackupManager.createBackup(masterPostgresDatabaseSettings, 0);
                    InputStream downloadedBackup = backupLoadManager.downloadBackup(backupProperties, testTaskID)
            ) {
                assertTrue(testUtils.streamsContentEquals(in, downloadedBackup));
            }
        }
    }
}
