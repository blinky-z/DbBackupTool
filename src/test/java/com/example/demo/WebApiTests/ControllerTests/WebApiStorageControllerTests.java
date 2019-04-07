package com.example.demo.WebApiTests.ControllerTests;

import com.example.demo.ApplicationTests;
import com.example.demo.repositories.StorageSettingsRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class WebApiStorageControllerTests extends ApplicationTests {
    @Autowired
    private StorageSettingsRepository storageSettingsRepository;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void createStorage_ShouldRespondWith400Error_WhenGotRequestToSaveDropboxSettingsWithAlreadyExistingSettingsName() {
        String settingsName =
                "createStorage_ShouldRespondWith400Error_WhenGotRequestToSaveDropboxSettingsWithAlreadyExistingSettingsName";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("storageType", "dropbox");
        body.add("settingsName", settingsName);
        body.add("dropboxSettings.accessToken", "createStorageTestAccessToken");

        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

        restTemplate.exchange("/storage", HttpMethod.POST, entity, String.class);

        ResponseEntity<String> responseEntity = restTemplate.exchange("/storage", HttpMethod.POST, entity, String.class);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    }

    @Test
    public void createStorage_ShouldSaveDropboxSettingsIntoDatabase_WhenGotRequestToSaveProperDropboxSettings() {
        String settingsName = "createStorage_ShouldSaveDropboxSettingsIntoDatabase_WhenGotRequestToSaveProperDropboxSettings";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("storageType", "dropbox");
        body.add("settingsName", settingsName);
        body.add("dropboxSettings.accessToken", "createStorageTestAccessToken");

        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> responseEntity = restTemplate.exchange("/storage", HttpMethod.POST, entity, String.class);
        assertEquals(HttpStatus.FOUND, responseEntity.getStatusCode());

        assertTrue(storageSettingsRepository.existsById(settingsName));
    }

    @Test
    public void createStorage_ShouldSaveLocalFileSystemSettingsIntoDatabase_WhenGotRequestToSaveProperLocalFileSystemSettings() {
        String settingsName =
                "createStorage_ShouldSaveLocalFileSystemSettingsIntoDatabase_WhenGotRequestToSaveProperLocalFileSystemSettings";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("storageType", "localFileSystem");
        body.add("settingsName", settingsName);
        body.add("localFileSystemSettings.backupPath", "createStorageTestBackupPath");

        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> responseEntity = restTemplate.exchange("/storage", HttpMethod.POST, entity, String.class);
        assertEquals(HttpStatus.FOUND, responseEntity.getStatusCode());

        assertTrue(storageSettingsRepository.existsById(settingsName));
    }

    @Test
    public void deleteStorage_ShouldRespondWith400Error_WhenStorageSettingsNameNotProvided() {
        ResponseEntity<String> responseEntity = restTemplate.exchange("/storage", HttpMethod.DELETE, null, String.class);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    }

    @Test
    public void deleteStorage_ShouldRespondWith400Error_WhenStorageSettingsNameParamNameProvidedButParamValueNot() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("settingsName", "");

        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> responseEntity = restTemplate.exchange("/storage", HttpMethod.DELETE, entity, String.class);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    }

    @Test
    public void deleteStorage_ShouldRespondWithFound_WhenStorageSettingsNameProvidedButNoSuchSettingsToDelete() {
        String settingsName = "deleteStorage_ShouldRespondWithFound_WhenStorageSettingsNameProvidedButNoSuchSettingsToDelete";

        {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, Object> createStorageBody = new LinkedMultiValueMap<>();
            createStorageBody.add("storageType", "localFileSystem");
            createStorageBody.add("settingsName", settingsName);
            createStorageBody.add("localFileSystemSettings.backupPath", "createStorageTestBackupPath");

            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(createStorageBody, headers);

            restTemplate.exchange("/storage", HttpMethod.POST, entity, String.class);
        }

        {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("settingsName", settingsName);

            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

            restTemplate.exchange("/storage", HttpMethod.DELETE, entity, String.class);
        }

        {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("settingsName", settingsName);

            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> responseEntity = restTemplate.exchange("/storage", HttpMethod.DELETE, entity, String.class);
            assertEquals(HttpStatus.FOUND, responseEntity.getStatusCode());
        }
    }

    @Test
    public void deleteStorage_ShouldDeleteStorageInDatabase_WhenGotRequestToDeleteProperStorage() {
        String settingsName = "deleteStorage_ShouldDeleteStorageInDatabase_WhenGotRequestToDeleteProperStorage";

        {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, Object> createStorageBody = new LinkedMultiValueMap<>();
            createStorageBody.add("storageType", "localFileSystem");
            createStorageBody.add("settingsName", settingsName);
            createStorageBody.add("localFileSystemSettings.backupPath", "createStorageTestBackupPath");

            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(createStorageBody, headers);

            ResponseEntity<String> responseEntity = restTemplate.exchange("/storage", HttpMethod.POST, entity, String.class);
            assertEquals(HttpStatus.FOUND, responseEntity.getStatusCode());

            assertTrue(storageSettingsRepository.findById(settingsName).isPresent());
        }

        {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("settingsName", settingsName);

            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> responseEntity = restTemplate.exchange("/storage", HttpMethod.DELETE, entity, String.class);
            assertEquals(HttpStatus.FOUND, responseEntity.getStatusCode());

            assertFalse(storageSettingsRepository.findById(settingsName).isPresent());
        }
    }
}
