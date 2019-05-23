package com.blog.controllers.WebApi.Validator;

import com.blog.ApplicationTests;
import com.blog.webUI.formTransfer.WebCreateBackupRequest;
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
class WebCreateBackupRequestValidatorTests extends ApplicationTests {
    @Autowired
    private WebCreateBackupRequestValidator webCreateBackupRequestValidator;

    @Test
    void validate_shouldRejectDatabaseSettingsNameField_whenMissingDatabaseSettingsName() {
        WebCreateBackupRequest webCreateBackupRequest = new WebCreateBackupRequest();

        Errors errors = new BeanPropertyBindingResult(webCreateBackupRequest, "");

        webCreateBackupRequestValidator.validate(webCreateBackupRequest, errors);

        assertTrue(errors.hasFieldErrors("databaseSettingsName"));
    }

    @Test
    void validate_shouldRejectBackupCreationPropertiesField_whenMissingBackupCreationProperties() {
        WebCreateBackupRequest webCreateBackupRequest = new WebCreateBackupRequest();

        Errors errors = new BeanPropertyBindingResult(webCreateBackupRequest, "");

        webCreateBackupRequestValidator.validate(webCreateBackupRequest, errors);

        assertTrue(errors.hasFieldErrors("backupCreationPropertiesMap"));
    }
}
