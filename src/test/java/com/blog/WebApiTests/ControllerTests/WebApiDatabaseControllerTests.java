package com.blog.WebApiTests.ControllerTests;

import com.blog.ApplicationTests;
import com.blog.entities.database.DatabaseSettings;
import com.blog.entities.database.DatabaseType;
import com.blog.repositories.DatabaseSettingsRepository;
import com.blog.webUI.formTransfer.WebAddDatabaseRequest;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
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

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
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

    static Matcher<DatabaseSettings> isEqualToDto(WebAddDatabaseRequest dto) {
        return new equalsToDto(dto);
    }

    @Test
    void createDatabase_ShouldSavePostgresSettingsIntoDatabase_WhenGotRequestToSaveProperPostgresSettings() {
        String settingsName =
                "createDatabase_ShouldSavePostgresSettingsIntoDatabase_WhenGotRequestToSaveProperPostgresSettings";

        WebAddDatabaseRequest request = controllersHttpClient.buildDefaultAddDatabaseRequest(
                DatabaseType.POSTGRES, settingsName);

        controllersHttpClient.addDatabase(request);

        Optional<DatabaseSettings> optionalDatabaseSettings = databaseSettingsRepository.findById(settingsName);
        assertTrue(optionalDatabaseSettings.isPresent());
        assertThat(optionalDatabaseSettings.get(), isEqualToDto(request));
    }

    @Test
    void deleteDatabase_ShouldDeleteSettings_WhenSendProperRequest() {
        String settingsName =
                "createDatabase_ShouldSavePostgresSettingsIntoDatabase_WhenGotRequestToSaveProperPostgresSettings";

        // create settings to subsequent delete
        {
            WebAddDatabaseRequest request = controllersHttpClient.buildDefaultAddDatabaseRequest(
                    DatabaseType.POSTGRES, settingsName);

            controllersHttpClient.addDatabase(request);
        }

        controllersHttpClient.deleteStorage(settingsName);

        assertFalse(databaseSettingsRepository.existsById(settingsName));
    }

    @Test
    void deleteDatabase_ShouldNotRespondWithError_WhenDatabaseSettingsNameProvidedButNoSuchSettingsToDelete() {
        String settingsName = "deleteDatabase_ShouldNotRespondWithError_WhenDatabaseSettingsNameProvidedButNoSuchSettingsToDelete";

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

        // delete settings again
        {
            ResponseEntity<String> responseEntity = controllersHttpClient.deleteDatabase(settingsName);
            assertEquals(HttpStatus.FOUND, responseEntity.getStatusCode());
        }
    }

    @Test
    void deleteDatabase_ShouldRespondWith400Error_WhenDatabaseSettingsNameNotProvided() {
        ResponseEntity<String> responseEntity = controllersHttpClient.deleteDatabase(null);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    }

    private static final class equalsToDto extends TypeSafeMatcher<DatabaseSettings> {
        private WebAddDatabaseRequest dto;

        equalsToDto(WebAddDatabaseRequest dto) {
            this.dto = dto;
        }

        @Override
        protected boolean matchesSafely(DatabaseSettings entity) {
            return entity.getSettingsName().equals(dto.getSettingsName()) &&
                    entity.getType().equals(DatabaseType.of(dto.getDatabaseType()).get()) &&
                    entity.getName().equals(dto.getDatabaseName())
                    && entity.getHost().equals(dto.getHost())
                    && entity.getPort() == Integer.valueOf(dto.getPort()) &&
                    entity.getLogin().equals(dto.getLogin())
                    && entity.getPassword().equals(dto.getPassword());
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("Entity should be equal to DTO: " + dto.toString());
        }
    }
}
