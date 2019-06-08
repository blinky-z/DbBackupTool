package com.blog.controllers.WebApi.Validator;

import com.blog.ApplicationTests;
import com.blog.webUI.formTransfer.WebRestoreBackupRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import static org.junit.jupiter.api.Assertions.*;

class WebRestoreBackupRequestValidatorTests extends ApplicationTests {
    @Autowired
    private WebRestoreBackupRequestValidator webRestoreBackupRequestValidator;

    @Test
    void validate_shouldRejectBackupIDField_whenMissingBackupID() {
        WebRestoreBackupRequest webRestoreBackupRequest = new WebRestoreBackupRequest();

        Errors errors = new BeanPropertyBindingResult(webRestoreBackupRequest, "");

        webRestoreBackupRequestValidator.validate(webRestoreBackupRequest, errors);

        assertTrue(errors.hasFieldErrors("backupId"));
        assertEquals("error.restoreBackupRequest.backupId.empty", errors.getFieldError("backupId").getCode());
    }

    @Test
    void validate_shouldRejectBackupIfField_whenBackupIdMalformed() {
        WebRestoreBackupRequest webRestoreBackupRequest = new WebRestoreBackupRequest();
        webRestoreBackupRequest.setBackupId("1d34f");

        Errors errors = new BeanPropertyBindingResult(webRestoreBackupRequest, "");

        webRestoreBackupRequestValidator.validate(webRestoreBackupRequest, errors);

        assertTrue(errors.hasFieldErrors("backupId"));
        assertEquals("error.restoreBackupRequest.backupId.malformed", errors.getFieldError("backupId").getCode());
    }

    @Test
    void validate_shouldRejectDatabaseSettingsName_whenMissingDatabaseSettingsName() {
        WebRestoreBackupRequest webRestoreBackupRequest = new WebRestoreBackupRequest();

        Errors errors = new BeanPropertyBindingResult(webRestoreBackupRequest, "");

        webRestoreBackupRequestValidator.validate(webRestoreBackupRequest, errors);

        assertTrue(errors.hasFieldErrors("databaseSettingsName"));
        assertEquals("error.restoreBackupRequest.databaseSettingsName.empty", errors.getFieldError("databaseSettingsName").getCode());
    }

    @Test
    void validate_shouldRejectStorageSettingsName_whenMissingStorageSettingsName() {
        WebRestoreBackupRequest webRestoreBackupRequest = new WebRestoreBackupRequest();

        Errors errors = new BeanPropertyBindingResult(webRestoreBackupRequest, "");

        webRestoreBackupRequestValidator.validate(webRestoreBackupRequest, errors);

        assertTrue(errors.hasFieldErrors("storageSettingsName"));
        assertEquals("error.restoreBackupRequest.storageSettingsName.empty", errors.getFieldError("storageSettingsName").getCode());
    }

    @Test
    void validate_shouldPass_whenPassProperDto(TestInfo testInfo) {
        WebRestoreBackupRequest webRestoreBackupRequest = new WebRestoreBackupRequest();
        webRestoreBackupRequest.setBackupId(String.valueOf(1));
        webRestoreBackupRequest.setDatabaseSettingsName(testInfo.getDisplayName());
        webRestoreBackupRequest.setStorageSettingsName(testInfo.getDisplayName());

        Errors errors = new BeanPropertyBindingResult(webRestoreBackupRequest, "");

        webRestoreBackupRequestValidator.validate(webRestoreBackupRequest, errors);

        assertFalse(errors.hasErrors());
    }
}