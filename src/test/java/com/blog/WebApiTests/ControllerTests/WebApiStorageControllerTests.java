package com.blog.WebApiTests.ControllerTests;

import com.blog.ApplicationTests;
import com.blog.entities.storage.StorageType;
import com.blog.repositories.StorageSettingsRepository;
import com.blog.webUI.formTransfer.WebAddStorageRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WebApiStorageControllerTests extends ApplicationTests {
    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private StorageSettingsRepository storageSettingsRepository;

    @Autowired
    private ControllersHttpClient controllersHttpClient;

    @BeforeAll
    void setup() {
        controllersHttpClient.setRestTemplate(restTemplate);
        controllersHttpClient.login();
    }

    @Test
    void createStorage_ShouldSaveStorageSettingsIntoDatabase_WhenGotRequestToSaveProperStorageSettings() {
        String settingsName = "createStorage_ShouldSaveStorageSettingsIntoDatabase_WhenGotRequestToSaveProperStorageSettings";

        WebAddStorageRequest request = controllersHttpClient.buildDefaultAddStorageRequest(StorageType.DROPBOX, settingsName);
        controllersHttpClient.addStorage(request);

        assertTrue(storageSettingsRepository.existsById(settingsName));
    }

    @Test
    void deleteStorage_ShouldRespondWith400Error_WhenStorageSettingsNameNotProvided() {
        ResponseEntity<String> responseEntity = controllersHttpClient.deleteStorage(null);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    }

    @Test
    void deleteStorage_ShouldRespondWithFound_WhenStorageSettingsNameProvidedButNoSuchSettingsToDelete() {
        String settingsName = "deleteStorage_ShouldRespondWithFound_WhenStorageSettingsNameProvidedButNoSuchSettingsToDelete";

        {
            WebAddStorageRequest request = controllersHttpClient.buildDefaultAddStorageRequest(StorageType.LOCAL_FILE_SYSTEM, settingsName);
            controllersHttpClient.addStorage(request);
        }

        {
            controllersHttpClient.deleteStorage(settingsName);
        }

        {
            ResponseEntity<String> responseEntity = controllersHttpClient.deleteStorage(settingsName);
            assertEquals(HttpStatus.FOUND, responseEntity.getStatusCode());
        }
    }

    @Test
    void deleteStorage_ShouldDeleteStorageFromDatabase_WhenGotRequestToDeleteProperStorage() {
        String settingsName = "deleteStorage_ShouldDeleteStorageInDatabase_WhenGotRequestToDeleteProperStorage";

        {
            WebAddStorageRequest request = controllersHttpClient.buildDefaultAddStorageRequest(StorageType.LOCAL_FILE_SYSTEM, settingsName);
            controllersHttpClient.addStorage(request);
        }

        {
            controllersHttpClient.deleteStorage(settingsName);

            assertFalse(storageSettingsRepository.existsById(settingsName));
        }
    }
}
