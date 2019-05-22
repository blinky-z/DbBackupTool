package com.blog.WebApiTests.ValidatorTests;

import com.blog.ApplicationTests;
import com.blog.controllers.WebApi.Validator.WebAddStorageRequestValidator;
import com.blog.webUI.formTransfer.WebAddStorageRequest;
import com.blog.webUI.formTransfer.storage.WebDropboxSettings;
import com.blog.webUI.formTransfer.storage.WebLocalFileSystemSettings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class WebAddStorageRequestValidatorTests extends ApplicationTests {
    @Autowired
    private WebAddStorageRequestValidator webAddStorageRequestValidator;

    /**
     * Construct WebAddStorageRequest object with minimum required fields set to test common fields
     *
     * @return new WebAddStorageRequest object to use in validator
     */
    private WebAddStorageRequest getWebAddStorageRequestForTestingCommonFields() {
        WebAddStorageRequest webAddStorageRequest = new WebAddStorageRequest();
        webAddStorageRequest.setStorageType("dropbox");
        return webAddStorageRequest;
    }

    /**
     * Construct WebAddStorageRequest object with minimum required fields set to test Dropbox storage specific fields
     *
     * @return new WebAddStorageRequest object to use in validator
     */
    private WebAddStorageRequest getWebAddStorageRequestForTestingDropboxFields() {
        WebAddStorageRequest webAddStorageRequest = new WebAddStorageRequest();
        webAddStorageRequest.setStorageType("dropbox");
        webAddStorageRequest.setSettingsName("testDropboxSettings");
        return webAddStorageRequest;
    }

    /**
     * Construct WebAddStorageRequest object with minimum required fields set to test Local File System storage specific fields
     *
     * @return new WebAddStorageRequest object to use in validator
     */
    private WebAddStorageRequest getWebAddStorageRequestForTestingLocalFileSystemFields() {
        WebAddStorageRequest webAddStorageRequest = new WebAddStorageRequest();
        webAddStorageRequest.setStorageType("localFileSystem");
        webAddStorageRequest.setSettingsName("testLocalFileSystemSettings");
        return webAddStorageRequest;
    }

    @Test
    void validate_ShouldRejectStorageTypeField_whenMissingStorageType() {
        WebAddStorageRequest webAddStorageRequest = new WebAddStorageRequest();

        Errors errors = new BeanPropertyBindingResult(webAddStorageRequest, "");

        webAddStorageRequestValidator.validate(webAddStorageRequest, errors);

        assertTrue(errors.hasFieldErrors("storageType"));
    }

    @Test
    void validate_ShouldRejectStorageTypeField_whenSetInvalidStorageType() {
        WebAddStorageRequest webAddStorageRequest = new WebAddStorageRequest();
        webAddStorageRequest.setStorageType("invalidStorage");

        Errors errors = new BeanPropertyBindingResult(webAddStorageRequest, "");

        webAddStorageRequestValidator.validate(webAddStorageRequest, errors);

        assertTrue(errors.hasFieldErrors("storageType"));
    }

    @Test
    void validate_ShouldRejectSettingsNameField_whenMissingSettingsName() {
        WebAddStorageRequest webAddStorageRequest = getWebAddStorageRequestForTestingCommonFields();

        Errors errors = new BeanPropertyBindingResult(webAddStorageRequest, "");

        webAddStorageRequestValidator.validate(webAddStorageRequest, errors);

        assertTrue(errors.hasFieldErrors("settingsName"));
    }

    @Test
    void validate_ShouldRejectRequiredDropboxFields_whenStorageSetButMissingStorageSpecificRequiredFields() {
        WebAddStorageRequest webAddStorageRequest = getWebAddStorageRequestForTestingDropboxFields();

        Errors errors = new BeanPropertyBindingResult(webAddStorageRequest, "");

        webAddStorageRequestValidator.validate(webAddStorageRequest, errors);

        assertTrue(errors.hasFieldErrors("dropboxSettings.accessToken"));
    }

    @Test
    void validate_ShouldRejectRequiredLocalFileSystemFields_whenStorageSetButMissingStorageSpecificRequiredFields() {
        WebAddStorageRequest webAddStorageRequest = getWebAddStorageRequestForTestingLocalFileSystemFields();

        Errors errors = new BeanPropertyBindingResult(webAddStorageRequest, "");

        webAddStorageRequestValidator.validate(webAddStorageRequest, errors);

        assertTrue(errors.hasFieldErrors("localFileSystemSettings.backupPath"));
    }

    @Test
    void validate_shouldPass_whenGivenProperDropboxSettings() {
        WebAddStorageRequest webAddStorageRequest = getWebAddStorageRequestForTestingDropboxFields();
        WebDropboxSettings webDropboxSettings = new WebDropboxSettings();
        webDropboxSettings.setAccessToken("testDropboxAccessToken");
        webAddStorageRequest.setDropboxSettings(webDropboxSettings);

        Errors errors = new BeanPropertyBindingResult(webAddStorageRequest, "");

        webAddStorageRequestValidator.validate(webAddStorageRequest, errors);

        assertFalse(errors.hasErrors());
    }

    @Test
    void validate_shouldPass_whenGivenProperLocalFileSystemSettings() {
        WebAddStorageRequest webAddStorageRequest = getWebAddStorageRequestForTestingLocalFileSystemFields();
        WebLocalFileSystemSettings webLocalFileSystemSettings = new WebLocalFileSystemSettings();
        webLocalFileSystemSettings.setBackupPath("testBackupPath");
        webAddStorageRequest.setLocalFileSystemSettings(webLocalFileSystemSettings);

        Errors errors = new BeanPropertyBindingResult(webAddStorageRequest, "");

        webAddStorageRequestValidator.validate(webAddStorageRequest, errors);

        assertFalse(errors.hasErrors());
    }
}
