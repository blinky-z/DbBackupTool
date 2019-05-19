package com.blog.WebApiTests.ControllerTests;

import com.blog.ApplicationTests;
import com.blog.TestUtils;
import com.blog.entities.backup.BackupProperties;
import com.blog.entities.backup.BackupTask;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class WebApiRestoreBackupControllerTests extends ApplicationTests {
    private static final java.util.List<String> tableNames = new ArrayList<>(Arrays.asList("comments"));
    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private BackupTaskManager backupTaskManager;
    @Autowired
    private TestUtils testUtils;
    @Autowired
    private JdbcTemplate jdbcPostgresMasterTemplate;
    @Autowired
    private JdbcTemplate jdbcPostgresCopyTemplate;
    @Autowired
    private BackupPropertiesManager backupPropertiesManager;
    @Autowired
    private MultiValueMap<String, Object> masterPostgresDatabaseSettingsAsMultiValueMap;
    @Autowired
    private MultiValueMap<String, Object> copyPostgresDatabaseSettingsAsMultiValueMap;
    @Autowired
    private MultiValueMap<String, Object> localFileSystemStorageSettingsAsMultiValueMap;
    @Autowired
    private MultiValueMap<String, Object> dropboxStorageSettingsAsMultiValueMap;

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

    @Before
    public void init() {
        testUtils.clearDatabase(jdbcPostgresMasterTemplate);
        testUtils.clearDatabase(jdbcPostgresCopyTemplate);
        addTables(jdbcPostgresMasterTemplate);
    }

    @Test
    public void givenProperRequestAndBackupIsLocatedOnLocalFileSystem_restoreBackup_shouldRestoreBackupSuccessfully_whenSendRequest() throws InterruptedException {
        String settingsName =
                "givenProperRequestAndBackupIsLocatedOnLocalFileSystem_restoreBackup_shouldRestoreBackupSuccessfully_whenSendRequest";

        {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>(localFileSystemStorageSettingsAsMultiValueMap);
            body.add("settingsName", settingsName);

            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> responseEntity = restTemplate.exchange("/storage", HttpMethod.POST, entity, String.class);

            assertEquals(HttpStatus.FOUND, responseEntity.getStatusCode());
        }

        String masterDatabaseSettingsName = settingsName + "_1";
        {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>(masterPostgresDatabaseSettingsAsMultiValueMap);
            body.add("settingsName", masterDatabaseSettingsName);

            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> responseEntity = restTemplate.exchange("/database", HttpMethod.POST, entity, String.class);

            assertEquals(HttpStatus.FOUND, responseEntity.getStatusCode());
        }

        // create backup of master database
        {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("databaseSettingsName", masterDatabaseSettingsName);
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

        String copyDatabaseSettingsName = settingsName + "_2";
        {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>(copyPostgresDatabaseSettingsAsMultiValueMap);
            body.add("settingsName", copyDatabaseSettingsName);

            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> responseEntity = restTemplate.exchange("/database", HttpMethod.POST, entity, String.class);

            assertEquals(HttpStatus.FOUND, responseEntity.getStatusCode());
        }

        // restore backup into copy database
        {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            Collection<BackupProperties> backupPropertiesCollection = backupPropertiesManager.findAllByOrderByIdDesc();
            BackupProperties backupProperties = Objects.requireNonNull(backupPropertiesCollection.iterator().next());

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("backupId", String.valueOf(backupProperties.getId()));
            body.add("databaseSettingsName", copyDatabaseSettingsName);

            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> responseEntity = restTemplate.exchange(
                    "/restore-backup", HttpMethod.POST, entity, String.class);

            assertEquals(HttpStatus.FOUND, responseEntity.getStatusCode());
        }

        // assert successful restoration
        {
            BackupTask backupTask = backupTaskManager.findAllByOrderByDateDesc().iterator().next();
            Integer id = backupTask.getId();

            while (backupTaskManager.getBackupTask(id).orElseThrow(RuntimeException::new).getState() != BackupTask.State.COMPLETED) {
                Thread.sleep(300);
            }

            testUtils.compareLargeTables(tableNames, jdbcPostgresMasterTemplate, jdbcPostgresCopyTemplate);
        }
    }

    @Test
    public void givenProperRequestAndBackupIsLocatedOnDropbox_restoreBackup_shouldRestoreBackupSuccessfully_whenSendRequest() throws InterruptedException {
        String settingsName =
                "givenProperRequestAndBackupIsLocatedOnDropbox_restoreBackup_shouldRestoreBackupSuccessfully_whenSendRequest";

        {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>(dropboxStorageSettingsAsMultiValueMap);
            body.add("settingsName", settingsName);

            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> responseEntity = restTemplate.exchange("/storage", HttpMethod.POST, entity, String.class);

            assertEquals(HttpStatus.FOUND, responseEntity.getStatusCode());
        }

        String masterDatabaseSettingsName = settingsName + "_1";
        {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>(masterPostgresDatabaseSettingsAsMultiValueMap);
            body.add("settingsName", masterDatabaseSettingsName);

            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> responseEntity = restTemplate.exchange("/database", HttpMethod.POST, entity, String.class);

            assertEquals(HttpStatus.FOUND, responseEntity.getStatusCode());
        }

        // create backup of master database
        {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("databaseSettingsName", masterDatabaseSettingsName);
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

        String copyDatabaseSettingsName = settingsName + "_2";
        {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>(copyPostgresDatabaseSettingsAsMultiValueMap);
            body.add("settingsName", copyDatabaseSettingsName);

            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> responseEntity = restTemplate.exchange("/database", HttpMethod.POST, entity, String.class);

            assertEquals(HttpStatus.FOUND, responseEntity.getStatusCode());
        }

        // restore backup into copy database
        {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            Collection<BackupProperties> backupPropertiesCollection = backupPropertiesManager.findAllByOrderByIdDesc();
            BackupProperties backupProperties = Objects.requireNonNull(backupPropertiesCollection.iterator().next());

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("backupId", String.valueOf(backupProperties.getId()));
            body.add("databaseSettingsName", copyDatabaseSettingsName);

            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> responseEntity = restTemplate.exchange(
                    "/restore-backup", HttpMethod.POST, entity, String.class);

            assertEquals(HttpStatus.FOUND, responseEntity.getStatusCode());
        }

        // assert successful restoration
        {
            BackupTask backupTask = backupTaskManager.findAllByOrderByDateDesc().iterator().next();
            Integer id = backupTask.getId();

            while (backupTaskManager.getBackupTask(id).orElseThrow(RuntimeException::new).getState() != BackupTask.State.COMPLETED) {
                Thread.sleep(300);
            }

            testUtils.compareLargeTables(tableNames, jdbcPostgresMasterTemplate, jdbcPostgresCopyTemplate);
        }
    }
}
