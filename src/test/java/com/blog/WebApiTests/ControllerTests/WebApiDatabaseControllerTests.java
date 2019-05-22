package com.blog.WebApiTests.ControllerTests;

import com.blog.ApplicationTests;
import com.blog.entities.database.DatabaseType;
import com.blog.repositories.DatabaseSettingsRepository;
import com.blog.webUI.formTransfer.WebAddDatabaseRequest;
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
class WebApiDatabaseControllerTests extends ApplicationTests {
    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private DatabaseSettingsRepository databaseSettingsRepository;

    @Autowired
    private ControllersHttpClient controllersHttpClient;

    @BeforeAll
    void setup() {
        controllersHttpClient.setRestTemplate(restTemplate);
        controllersHttpClient.login();
    }

    @Test
    void createDatabase_ShouldSavePostgresSettingsIntoDatabase_WhenGotRequestToSaveProperPostgresSettings() {
        String settingsName =
                "createDatabase_ShouldSavePostgresSettingsIntoDatabase_WhenGotRequestToSaveProperPostgresSettings";

        WebAddDatabaseRequest request = controllersHttpClient.buildDefaultAddDatabaseRequest(
                DatabaseType.POSTGRES, settingsName);

        controllersHttpClient.addDatabase(request);

        assertTrue(databaseSettingsRepository.existsById(settingsName));
    }

    @Test
    void deleteDatabase_ShouldRespondWith400Error_WhenDatabaseSettingsNameNotProvided() {
        ResponseEntity<String> responseEntity = controllersHttpClient.deleteDatabase(null);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    }

    @Test
    void deleteDatabase_ShouldRespondWithFound_WhenDatabaseSettingsNameProvidedButNoSuchSettingsToDelete() {
        String settingsName = "deleteDatabase_ShouldRespondWithFound_WhenDatabaseSettingsNameProvidedButNoSuchSettingsToDelete";

        // create settings
        {
            WebAddDatabaseRequest request = controllersHttpClient.buildDefaultAddDatabaseRequest(
                    DatabaseType.POSTGRES, settingsName);

            controllersHttpClient.addDatabase(request);
        }

        // delete settings
        {
            controllersHttpClient.deleteDatabase(settingsName);
            assertFalse(databaseSettingsRepository.existsById(settingsName));
        }

        // try to delete settings again
        {
            ResponseEntity<String> responseEntity = controllersHttpClient.deleteDatabase(settingsName);
            assertEquals(HttpStatus.FOUND, responseEntity.getStatusCode());
        }
    }
}
