package com.blog.controllers.WebApi.Validator;

import com.blog.ApplicationTests;
import com.blog.webUI.formTransfer.WebCreateBackupRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class WebCreateBackupRequestValidatorTests extends ApplicationTests {
    @Autowired
    private WebCreateBackupRequestValidator webCreateBackupRequestValidator;

    @Test
    void validate_shouldRejectDatabaseSettingsNameField_whenMissingDatabaseSettingsName() {
        WebCreateBackupRequest webCreateBackupRequest = new WebCreateBackupRequest();

        Errors errors = new BeanPropertyBindingResult(webCreateBackupRequest, "");

        webCreateBackupRequestValidator.validate(webCreateBackupRequest, errors);

        assertTrue(errors.hasFieldErrors("databaseSettingsName"));
        assertEquals("error.createBackupRequest.databaseSettingsName.empty", errors.getFieldError("databaseSettingsName").getCode());
    }

    @Test
    void validate_shouldRejectstorageSettingsNameListField_whenMissingstorageSettingsNameList() {
        WebCreateBackupRequest webCreateBackupRequest = new WebCreateBackupRequest();

        Errors errors = new BeanPropertyBindingResult(webCreateBackupRequest, "");

        webCreateBackupRequestValidator.validate(webCreateBackupRequest, errors);

        assertTrue(errors.hasFieldErrors("storageSettingsNameList"));
        assertEquals("error.createBackupRequest.storageSettingsNameList.empty", errors.getFieldError("storageSettingsNameList").getCode());
    }

    @Test
    void validate_shouldPass_whenPassProperDto(TestInfo testInfo) {
        WebCreateBackupRequest webCreateBackupRequest = new WebCreateBackupRequest();
        webCreateBackupRequest.setDatabaseSettingsName(testInfo.getDisplayName());
        webCreateBackupRequest.setStorageSettingsNameList(Collections.singletonList(testInfo.getDisplayName()));
        webCreateBackupRequest.setProcessors(Arrays.asList("processor1", "processor2"));

        Errors errors = new BeanPropertyBindingResult(webCreateBackupRequest, "");

        webCreateBackupRequestValidator.validate(webCreateBackupRequest, errors);

        assertFalse(errors.hasErrors());
    }
}
