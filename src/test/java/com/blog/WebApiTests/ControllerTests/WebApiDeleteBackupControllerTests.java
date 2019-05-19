package com.blog.WebApiTests.ControllerTests;

import com.blog.ApplicationTests;
import com.blog.TestUtils;
import com.blog.entities.backup.BackupProperties;
import com.blog.entities.backup.BackupTask;
import com.blog.entities.storage.StorageType;
import com.blog.manager.BackupPropertiesManager;
import com.blog.manager.BackupTaskManager;
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

import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class WebApiDeleteBackupControllerTests extends ApplicationTests {
    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TestUtils testUtils;

    @Autowired
    private JdbcTemplate jdbcPostgresMasterTemplate;

    @Autowired
    private BackupPropertiesManager backupPropertiesManager;

    @Autowired
    private BackupTaskManager backupTaskManager;

    @Autowired
    private MultiValueMap<String, Object> masterPostgresDatabaseSettingsAsMultiValueMap;

    @Autowired
    private MultiValueMap<String, Object> localFileSystemStorageSettingsAsMultiValueMap;

    @Autowired
    private MultiValueMap<String, Object> dropboxStorageSettingsAsMultiValueMap;

    @Before
    public void init() {
        testUtils.clearDatabase(jdbcPostgresMasterTemplate);
        testUtils.initDatabase(jdbcPostgresMasterTemplate);
    }

    @Test
    public void givenBackupSavedOnLocalFileSystem_deleteBackup_shouldDeleteBackup() throws InterruptedException {
        String settingsName = "givenBackupSavedOnLocalFileSystem_deleteBackup_shouldDeleteBackup";

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
        }

        {
            Collection<BackupProperties> backupPropertiesCollection = backupPropertiesManager.findAllByOrderByIdDesc();
            BackupProperties backupProperties = backupPropertiesCollection.iterator().next();
            Integer backupId = backupProperties.getId();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("backupId", String.valueOf(backupId));

            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> responseEntity = restTemplate.exchange("/delete-backup", HttpMethod.DELETE, entity, String.class);

            assertEquals(HttpStatus.FOUND, responseEntity.getStatusCode());

            BackupTask deleteTask = backupTaskManager.findAllByOrderByDateDesc().iterator().next();
            Integer id = deleteTask.getId();

            while (backupTaskManager.getBackupTask(id).orElseThrow(RuntimeException::new).getState() != BackupTask.State.COMPLETED) {
                Thread.sleep(300);
            }

            assertFalse(backupPropertiesManager.existsById(backupId));
            assertFalse(testUtils.backupExistsOnStorage(StorageType.LOCAL_FILE_SYSTEM, backupProperties.getBackupName()));
        }
    }

    @Test
    public void givenBackupSavedOnDropbox_deleteBackup_shouldDeleteBackup() throws InterruptedException {
        String settingsName = "givenBackupSavedOnDropbox_deleteBackup_shouldDeleteBackup";

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
        }

        {
            Collection<BackupProperties> backupPropertiesCollection = backupPropertiesManager.findAllByOrderByIdDesc();
            BackupProperties backupProperties = backupPropertiesCollection.iterator().next();
            Integer backupId = backupProperties.getId();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("backupId", String.valueOf(backupId));

            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> responseEntity = restTemplate.exchange("/delete-backup", HttpMethod.DELETE, entity, String.class);

            assertEquals(HttpStatus.FOUND, responseEntity.getStatusCode());

            BackupTask deleteTask = backupTaskManager.findAllByOrderByDateDesc().iterator().next();
            Integer id = deleteTask.getId();

            while (backupTaskManager.getBackupTask(id).orElseThrow(RuntimeException::new).getState() != BackupTask.State.COMPLETED) {
                Thread.sleep(300);
            }

            assertFalse(backupPropertiesManager.existsById(backupId));
            assertFalse(testUtils.backupExistsOnStorage(StorageType.DROPBOX, backupProperties.getBackupName()));
        }
    }
}
