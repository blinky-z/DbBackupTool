package com.blog.WebApiTests.ControllerTests;

import com.blog.ApplicationTests;
import com.blog.TestUtils;
import com.blog.entities.backup.BackupProperties;
import com.blog.entities.database.DatabaseSettings;
import com.blog.manager.BackupLoadManager;
import com.blog.manager.BackupPropertiesManager;
import com.blog.manager.DatabaseBackupManager;
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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class WebApiRestoreBackupControllerTests extends ApplicationTests {
    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TestUtils testUtils;

    @Autowired
    private JdbcTemplate jdbcPostgresMasterTemplate;

    @Autowired
    private JdbcTemplate jdbcPostgresCopyTemplate;

    @Autowired
    private DatabaseSettings copyPostgresDatabaseSettings;

    @Autowired
    private DatabaseBackupManager databaseBackupManager;

    @Autowired
    private BackupPropertiesManager backupPropertiesManager;

    @Autowired
    private BackupLoadManager backupLoadManager;

    @Autowired
    private MultiValueMap<String, Object> postgresDatabaseSettingsAsMultiValueMap;

    @Autowired
    private MultiValueMap<String, Object> localFileSystemStorageSettingsAsMultiValueMap;

    @Autowired
    private MultiValueMap<String, Object> dropboxStorageSettingsAsMultiValueMap;

    private static final java.util.List<String> tableNames = new ArrayList<>(Arrays.asList("comments"));

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
    public void givenProperRequestAndBackupIsLocatedOnLocalFileSystem_restoreBackup_shouldRestoreBackupSuccessfully_whenSendRequest() {
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

        {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>(postgresDatabaseSettingsAsMultiValueMap);
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
        }

        // restore backup
        {
            Collection<BackupProperties> backupPropertiesCollection = backupPropertiesManager.findAllByOrderByIdDesc();
            BackupProperties backupProperties = Objects.requireNonNull(backupPropertiesCollection.iterator().next());

            InputStream downloadedBackup = backupLoadManager.downloadBackup(backupProperties);
            databaseBackupManager.restoreBackup(downloadedBackup, copyPostgresDatabaseSettings);

            testUtils.compareLargeTables(tableNames, jdbcPostgresMasterTemplate, jdbcPostgresCopyTemplate);
        }
    }

    @Test
    public void givenProperRequestAndBackupIsLocatedOnDropbox_restoreBackup_shouldRestoreBackupSuccessfully_whenSendRequest() {
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

        {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>(postgresDatabaseSettingsAsMultiValueMap);
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
        }

        // restore backup
        {
            Collection<BackupProperties> backupPropertiesCollection = backupPropertiesManager.findAllByOrderByIdDesc();
            BackupProperties backupProperties = Objects.requireNonNull(backupPropertiesCollection.iterator().next());

            InputStream downloadedBackup = backupLoadManager.downloadBackup(backupProperties);
            databaseBackupManager.restoreBackup(downloadedBackup, copyPostgresDatabaseSettings);

            testUtils.compareLargeTables(tableNames, jdbcPostgresMasterTemplate, jdbcPostgresCopyTemplate);
        }
    }
}
