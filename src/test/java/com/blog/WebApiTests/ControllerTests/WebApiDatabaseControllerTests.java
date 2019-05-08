package com.blog.WebApiTests.ControllerTests;

import com.blog.ApplicationTests;
import com.blog.repositories.DatabaseSettingsRepository;
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
public class WebApiDatabaseControllerTests extends ApplicationTests {
    @Autowired
    private DatabaseSettingsRepository databaseSettingsRepository;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private MultiValueMap<String, Object> masterPostgresDatabaseSettingsAsMultiValueMap;

    @Test
    public void createDatabase_ShouldRespondWith400Error_WhenGotRequestToSavePostgresSettingsWithAlreadyExistingSettingsName() {
        String settingsName =
                "createDatabase_ShouldRespondWith400Error_WhenGotRequestToSavePostgresSettingsWithAlreadyExistingSettingsName";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>(masterPostgresDatabaseSettingsAsMultiValueMap);
        body.add("settingsName", settingsName);

        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

        restTemplate.exchange("/database", HttpMethod.POST, entity, String.class);

        ResponseEntity<String> responseEntity = restTemplate.exchange("/database", HttpMethod.POST, entity, String.class);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    }

    @Test
    public void createDatabase_ShouldSavePostgresSettingsIntoDatabase_WhenGotRequestToSaveProperPostgresSettings() {
        String settingsName =
                "createDatabase_ShouldSavePostgresSettingsIntoDatabase_WhenGotRequestToSaveProperPostgresSettings";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>(masterPostgresDatabaseSettingsAsMultiValueMap);
        body.add("settingsName", settingsName);

        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> responseEntity = restTemplate.exchange("/database", HttpMethod.POST, entity, String.class);

        assertEquals(HttpStatus.FOUND, responseEntity.getStatusCode());
        assertTrue(databaseSettingsRepository.existsById(settingsName));
    }

    @Test
    public void deleteDatabase_ShouldRespondWith400Error_WhenDatabaseSettingsNameNotProvided() {
        ResponseEntity<String> responseEntity =
                restTemplate.exchange("/database", HttpMethod.DELETE, null, String.class);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    }

    @Test
    public void deleteDatabase_ShouldRespondWith400Error_WhenDatabaseSettingsNameParamNameProvidedButParamValueNot() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("settingsName", "");

        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> responseEntity = restTemplate.exchange("/database", HttpMethod.DELETE, entity, String.class);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    }

    @Test
    public void deleteDatabase_ShouldRespondWithFound_WhenDatabaseSettingsNameProvidedButNoSuchSettingsToDelete() {
        String settingsName = "deleteDatabase_ShouldRespondWithFound_WhenDatabaseSettingsNameProvidedButNoSuchSettingsToDelete";

        // create settings
        {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>(masterPostgresDatabaseSettingsAsMultiValueMap);
            body.add("settingsName", settingsName);

            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

            restTemplate.exchange("/database", HttpMethod.POST, entity, String.class);
            assertTrue(databaseSettingsRepository.existsById(settingsName));
        }

        // delete settings
        {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("settingsName", settingsName);

            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

            restTemplate.exchange("/database", HttpMethod.DELETE, entity, String.class);
            assertFalse(databaseSettingsRepository.existsById(settingsName));
        }

        // try to delete settings again
        {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("settingsName", settingsName);

            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> responseEntity = restTemplate.exchange("/database", HttpMethod.DELETE, entity, String.class);
            assertEquals(HttpStatus.FOUND, responseEntity.getStatusCode());
        }
    }
}
