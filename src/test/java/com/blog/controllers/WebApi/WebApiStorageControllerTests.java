package com.blog.controllers.WebApi;

import com.blog.ApplicationTests;
import com.blog.entities.storage.DropboxSettings;
import com.blog.entities.storage.LocalFileSystemSettings;
import com.blog.entities.storage.StorageSettings;
import com.blog.entities.storage.StorageType;
import com.blog.repositories.StorageSettingsRepository;
import com.blog.webUI.formTransfer.WebAddStorageRequest;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WebApiStorageControllerTests extends ApplicationTests {
    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private StorageSettingsRepository storageSettingsRepository;

    @Autowired
    private WebApiClient webApiClient;

    private static Matcher<StorageSettings> isEqualToDto(WebAddStorageRequest dto) {
        return new equalsToDto(dto);
    }

    @BeforeAll
    void setup() {
        webApiClient.setRestTemplate(restTemplate);
        webApiClient.login();
    }

    @Test
    void createStorage_ShouldSaveLocalFileSystemStorageSettingsIntoDatabase_WhenGotRequestToSaveProperRequest() {
        String settingsName = "createStorage_ShouldSaveLocalFileSystemStorageSettingsIntoDatabase_WhenGotRequestToSaveProperRequest";

        WebAddStorageRequest request = webApiClient.buildDefaultAddStorageRequest(StorageType.LOCAL_FILE_SYSTEM, settingsName);
        webApiClient.addStorage(request);

        Optional<StorageSettings> optionalStorageSettings = storageSettingsRepository.findById(settingsName);
        assertTrue(optionalStorageSettings.isPresent());
        assertThat(optionalStorageSettings.get(), isEqualToDto(request));
    }

    @Test
    void createStorage_ShouldSaveDropboxStorageSettingsIntoDatabase_WhenGotRequestToSaveProperRequest() {
        String settingsName = "createStorage_ShouldSaveDropboxStorageSettingsIntoDatabase_WhenGotRequestToSaveProperRequest";

        WebAddStorageRequest request = webApiClient.buildDefaultAddStorageRequest(StorageType.DROPBOX, settingsName);
        webApiClient.addStorage(request);

        Optional<StorageSettings> optionalStorageSettings = storageSettingsRepository.findById(settingsName);
        assertTrue(optionalStorageSettings.isPresent());
        assertThat(optionalStorageSettings.get(), isEqualToDto(request));
    }

    @Test
    void deleteStorage_ShouldDeleteSettings_WhenSendProperRequest() {
        String settingsName =
                "deleteStorage_ShouldDeleteSettings_WhenSendProperRequest";

        // create settings to subsequent delete
        {
            WebAddStorageRequest request = webApiClient.buildDefaultAddStorageRequest(
                    StorageType.LOCAL_FILE_SYSTEM, settingsName);

            webApiClient.addStorage(request);
        }

        webApiClient.deleteStorage(settingsName);

        assertFalse(storageSettingsRepository.existsById(settingsName));
    }

    @Test
    void deleteStorage_ShouldNotRespondWithError_WhenStorageSettingsNameProvidedButNoSuchSettingsToDelete() {
        String settingsName = "deleteStorage_ShouldNotRespondWithError_WhenStorageSettingsNameProvidedButNoSuchSettingsToDelete";

        // create settings
        {
            WebAddStorageRequest request = webApiClient.buildDefaultAddStorageRequest(StorageType.LOCAL_FILE_SYSTEM, settingsName);
            webApiClient.addStorage(request);
        }

        // delete settings
        {
            webApiClient.deleteStorage(settingsName);
        }

        // delete settings again
        {
            ResponseEntity<String> responseEntity = webApiClient.deleteStorage(settingsName);
            assertEquals(HttpStatus.FOUND, responseEntity.getStatusCode());
        }
    }

    @Test
    void deleteStorage_ShouldRespondWith400Error_WhenStorageSettingsNameNotProvided() {
        ResponseEntity<String> responseEntity = webApiClient.deleteStorage(null);
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    }

    private static final class equalsToDto extends TypeSafeMatcher<StorageSettings> {
        private WebAddStorageRequest dto;

        equalsToDto(WebAddStorageRequest dto) {
            this.dto = dto;
        }

        @Override
        protected boolean matchesSafely(StorageSettings entity) {
            if (!entity.getSettingsName().equals(dto.getSettingsName())) {
                return false;
            }

            StorageType storageType = entity.getType();
            if (!storageType.equals(StorageType.of(dto.getStorageType()).get())) {
                return false;
            }

            switch (storageType) {
                case LOCAL_FILE_SYSTEM: {
                    LocalFileSystemSettings localFileSystemSettings = entity.getLocalFileSystemSettings().get();
                    if (!localFileSystemSettings.getBackupPath().equals(dto.getLocalFileSystemSettings().getBackupPath())) {
                        return false;
                    }
                    break;
                }
                case DROPBOX: {
                    DropboxSettings dropboxSettings = entity.getDropboxSettings().get();
                    if (!dropboxSettings.getAccessToken().equals(dto.getDropboxSettings().getAccessToken())) {
                        return false;
                    }
                    break;
                }
                default: {
                    return false;
                }
            }

            return true;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText("Entity should be equal to DTO: " + dto.toString());
        }
    }
}
