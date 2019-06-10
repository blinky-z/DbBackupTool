package com.blog.controllers.WebApi;

import com.blog.ApplicationTests;
import com.blog.entities.database.DatabaseSettings;
import com.blog.entities.database.DatabaseType;
import com.blog.repositories.DatabaseSettingsRepository;
import com.blog.webUI.formTransfer.WebAddDatabaseRequest;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class WebApiDatabaseControllerTests extends ApplicationTests {
    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private DatabaseSettingsRepository databaseSettingsRepository;

    @Autowired
    private WebApiClient webApiClient;

    private final AtomicBoolean initialized = new AtomicBoolean(false);

    private static Matcher<DatabaseSettings> isEqualToDto(WebAddDatabaseRequest dto) {
        return new equalsToDto(dto);
    }

    @Test
    void createDatabase_ShouldSavePostgresSettingsIntoDatabase_WhenGotRequestToSaveProperPostgresSettings() {
        String settingsName =
                "createDatabase_ShouldSavePostgresSettingsIntoDatabase_WhenGotRequestToSaveProperPostgresSettings";

        WebAddDatabaseRequest request = webApiClient.buildDefaultAddDatabaseRequest(
                DatabaseType.POSTGRES, settingsName);

        webApiClient.addDatabase(request);

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
            WebAddDatabaseRequest request = webApiClient.buildDefaultAddDatabaseRequest(
                    DatabaseType.POSTGRES, settingsName);

            webApiClient.addDatabase(request);
        }

        webApiClient.deleteDatabase(settingsName);

        assertFalse(databaseSettingsRepository.existsById(settingsName));
    }

    @Test
    void deleteDatabase_ShouldRespondWith400Error_WhenDatabaseSettingsNameNotProvided() {
        ResponseEntity<String> responseEntity = webApiClient.deleteDatabase(null);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    }

    @Test
    void deleteDatabase_ShouldNotRespondWithError_WhenDatabaseSettingsNameProvidedButNoSuchSettingsToDelete() {
        String settingsName = "deleteDatabase_ShouldNotRespondWithError_WhenDatabaseSettingsNameProvidedButNoSuchSettingsToDelete";

        // create settings
        {
            WebAddDatabaseRequest request = webApiClient.buildDefaultAddDatabaseRequest(
                    DatabaseType.POSTGRES, settingsName);

            webApiClient.addDatabase(request);
        }

        // delete settings
        {
            webApiClient.deleteDatabase(settingsName);
            assertFalse(databaseSettingsRepository.existsById(settingsName));
        }

        // delete settings again
        {
            ResponseEntity<String> responseEntity = webApiClient.deleteDatabase(settingsName);
            assertEquals(HttpStatus.FOUND, responseEntity.getStatusCode());
        }
    }

    @BeforeEach
    void init() {
        if (initialized.compareAndSet(false, true)) {
            webApiClient.setTestRestTemplate(restTemplate);
        }
    }

    private static final class equalsToDto extends TypeSafeMatcher<DatabaseSettings> {
        private WebAddDatabaseRequest dto;

        equalsToDto(WebAddDatabaseRequest dto) {
            this.dto = dto;
        }

        @Override
        protected boolean matchesSafely(DatabaseSettings entity) {
            return entity.getSettingsName().equals(dto.getSettingsName())
                    && entity.getType().equals(DatabaseType.of(dto.getDatabaseType()).get())
                    && entity.getName().equals(dto.getDatabaseName())
                    && entity.getHost().equals(dto.getHost())
                    && entity.getPort() == Integer.valueOf(dto.getPort())
                    && entity.getLogin().equals(dto.getLogin())
                    && entity.getPassword().equals(dto.getPassword());
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("Entity should be equal to DTO").appendValue(dto);
        }

        @Override
        protected void describeMismatchSafely(DatabaseSettings item, Description mismatchDescription) {
            mismatchDescription.appendText("was").appendValue(item);
        }
    }
}
