package com.blog.WebApiTests.ValidatorTests;

import com.blog.ApplicationTests;
import com.blog.controllers.WebApi.Validator.WebRestoreBackupRequestValidator;
import com.blog.webUI.formTransfer.WebRestoreBackupRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class WebRestoreBackupRequestValidatorTests extends ApplicationTests {
    @Autowired
    private WebRestoreBackupRequestValidator webRestoreBackupRequestValidator;

    @Test
    void validate_shouldRejectBackupIfField_whenMissingBackupId() {
        WebRestoreBackupRequest webRestoreBackupRequest = new WebRestoreBackupRequest();

        Errors errors = new BeanPropertyBindingResult(webRestoreBackupRequest, "");

        webRestoreBackupRequestValidator.validate(webRestoreBackupRequest, errors);

        assertTrue(errors.hasFieldErrors("backupId"));
    }

    @Test
    void validate_shouldRejectBackupIfField_whenBackupIdMalformed() {
        WebRestoreBackupRequest webRestoreBackupRequest = new WebRestoreBackupRequest();
        webRestoreBackupRequest.setBackupId("134f");

        Errors errors = new BeanPropertyBindingResult(webRestoreBackupRequest, "");

        webRestoreBackupRequestValidator.validate(webRestoreBackupRequest, errors);

        assertTrue(errors.hasFieldErrors("backupId"));
    }

    @Test
    void validate_shouldRejectDatabaseSettingsName_whenMissingDatabaseSettingsName() {
        WebRestoreBackupRequest webRestoreBackupRequest = new WebRestoreBackupRequest();

        Errors errors = new BeanPropertyBindingResult(webRestoreBackupRequest, "");

        webRestoreBackupRequestValidator.validate(webRestoreBackupRequest, errors);

        assertTrue(errors.hasFieldErrors("databaseSettingsName"));
    }
}