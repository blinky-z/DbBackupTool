package com.blog.controllers.WebApi.Validator;

import com.blog.ApplicationTests;
import com.blog.controllers.Errors.ValidationException;
import com.blog.entities.storage.StorageType;
import com.blog.webUI.formTransfer.WebAddStorageRequest;
import com.blog.webUI.formTransfer.storage.WebDropboxSettings;
import com.blog.webUI.formTransfer.storage.WebLocalFileSystemSettings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import static org.junit.jupiter.api.Assertions.*;

class WebAddStorageRequestValidatorTests extends ApplicationTests {
    private WebAddStorageRequestValidator webAddStorageRequestValidator = new WebAddStorageRequestValidator();

    @Test
    void validate_ShouldRejectStorageTypeField_whenMissingStorageType() {
        WebAddStorageRequest webAddStorageRequest = new WebAddStorageRequest();

        Errors errors = new BeanPropertyBindingResult(webAddStorageRequest, "");

        webAddStorageRequestValidator.validate(webAddStorageRequest, errors);

        assertTrue(errors.hasFieldErrors("storageType"));
        assertEquals("error.addStorageRequest.storageType.empty", errors.getFieldError("storageType").getCode());
    }

    @Test
    void validate_ShouldRejectStorageTypeField_whenPassedInvalidStorageType() {
        WebAddStorageRequest webAddStorageRequest = new WebAddStorageRequest();
        webAddStorageRequest.setStorageType("invalidStorage");

        Errors errors = new BeanPropertyBindingResult(webAddStorageRequest, "");

        webAddStorageRequestValidator.validate(webAddStorageRequest, errors);

        assertTrue(errors.hasFieldErrors("storageType"));
        assertEquals("error.addStorageRequest.storageType.malformed", errors.getFieldError("storageType").getCode());
    }

    @Test
    void validate_ShouldRejectSettingsNameField_whenMissingSettingsName() {
        WebAddStorageRequest webAddStorageRequest = new WebAddStorageRequest();

        Errors errors = new BeanPropertyBindingResult(webAddStorageRequest, "");

        webAddStorageRequestValidator.validate(webAddStorageRequest, errors);

        assertTrue(errors.hasFieldErrors("settingsName"));
        assertEquals("error.addStorageRequest.settingsName.empty", errors.getFieldError("settingsName").getCode());
    }

    @Test
    void validate_ShouldRejectRequiredDropboxFields_whenCorrespondingStorageIsSetButMissingStorageSpecificRequiredFields() {
        WebAddStorageRequest webAddStorageRequest = new WebAddStorageRequest();
        webAddStorageRequest.setStorageType(StorageType.DROPBOX.getStorageAsString());

        Errors errors = new BeanPropertyBindingResult(webAddStorageRequest, "");

        webAddStorageRequestValidator.validate(webAddStorageRequest, errors);

        assertTrue(errors.hasFieldErrors("dropboxSettings.accessToken"));
        assertEquals("error.addStorageRequest.dropboxSettings.accessToken.empty",
                errors.getFieldError("dropboxSettings.accessToken").getCode());
    }

    @Test
    void validate_ShouldRejectRequiredLocalFileSystemFields_whenCorrespondingStorageIsSetButMissingStorageSpecificRequiredFields() {
        WebAddStorageRequest webAddStorageRequest = new WebAddStorageRequest();
        webAddStorageRequest.setStorageType(StorageType.LOCAL_FILE_SYSTEM.getStorageAsString());

        Errors errors = new BeanPropertyBindingResult(webAddStorageRequest, "");

        webAddStorageRequestValidator.validate(webAddStorageRequest, errors);

        assertTrue(errors.hasFieldErrors("localFileSystemSettings.backupPath"));
        assertEquals("error.addStorageRequest.localFileSystemSettings.backupPath.empty",
                errors.getFieldError("localFileSystemSettings.backupPath").getCode());
    }

    @Test
    void validate_shouldPass_whenGivenProperDropboxSettings(TestInfo testInfo) {
        WebAddStorageRequest webAddStorageRequest = new WebAddStorageRequest();
        webAddStorageRequest.setStorageType(StorageType.DROPBOX.getStorageAsString());
        webAddStorageRequest.setSettingsName(testInfo.getDisplayName());

        WebDropboxSettings webDropboxSettings = new WebDropboxSettings();
        webDropboxSettings.setAccessToken("testDropboxAccessToken");

        webAddStorageRequest.setDropboxSettings(webDropboxSettings);

        Errors errors = new BeanPropertyBindingResult(webAddStorageRequest, "");

        webAddStorageRequestValidator.validate(webAddStorageRequest, errors);

        assertFalse(errors.hasErrors());
    }

    @Test
    void validate_shouldPass_whenGivenProperLocalFileSystemSettings(TestInfo testInfo) {
        WebAddStorageRequest webAddStorageRequest = new WebAddStorageRequest();
        webAddStorageRequest.setStorageType(StorageType.LOCAL_FILE_SYSTEM.getStorageAsString());
        webAddStorageRequest.setSettingsName(testInfo.getDisplayName());

        WebLocalFileSystemSettings webLocalFileSystemSettings = new WebLocalFileSystemSettings();
        webLocalFileSystemSettings.setBackupPath("/home/backup");

        webAddStorageRequest.setLocalFileSystemSettings(webLocalFileSystemSettings);

        Errors errors = new BeanPropertyBindingResult(webAddStorageRequest, "");

        webAddStorageRequestValidator.validate(webAddStorageRequest, errors);

        assertFalse(errors.hasErrors());
    }

    @Test
    void validate_shouldNotThrowValidationException_whenPassAnyStorageType(TestInfo testInfo) {
        for (StorageType storage : StorageType.values()) {
            WebAddStorageRequest webAddStorageRequest = new WebAddStorageRequest();
            webAddStorageRequest.setStorageType(storage.getStorageAsString());

            Errors errors = new BeanPropertyBindingResult(webAddStorageRequest, "");

            try {
                webAddStorageRequestValidator.validateStorageSpecificFields(webAddStorageRequest, errors);
            } catch (ValidationException ex) {
                fail("Exception must not be thrown on storage type " + storage);
            }
            assertFalse(errors.hasFieldErrors("storageType"));
        }
    }
}
