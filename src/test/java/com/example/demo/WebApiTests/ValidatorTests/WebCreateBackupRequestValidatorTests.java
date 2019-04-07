package com.example.demo.WebApiTests.ValidatorTests;

import com.example.demo.ApplicationTests;
import com.example.demo.controllers.WebApi.Validator.WebCreateBackupRequestValidator;
import com.example.demo.webUI.formTransfer.WebCreateBackupRequest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest
public class WebCreateBackupRequestValidatorTests extends ApplicationTests {
    @Autowired
    private WebCreateBackupRequestValidator webCreateBackupRequestValidator;

    @Test
    public void validate_shouldRejectDatabaseSettingsNameField_whenMissingDatabaseSettingsName() {
        WebCreateBackupRequest webCreateBackupRequest = new WebCreateBackupRequest();

        Errors errors = new BeanPropertyBindingResult(webCreateBackupRequest, "");

        webCreateBackupRequestValidator.validate(webCreateBackupRequest, errors);

        assertTrue(errors.hasFieldErrors("databaseSettingsName"));
    }

    @Test
    public void validate_shouldRejectBackupCreationPropertiesField_whenMissingBackupCreationProperties() {
        WebCreateBackupRequest webCreateBackupRequest = new WebCreateBackupRequest();

        Errors errors = new BeanPropertyBindingResult(webCreateBackupRequest, "");

        webCreateBackupRequestValidator.validate(webCreateBackupRequest, errors);

        assertTrue(errors.hasFieldErrors("backupCreationProperties"));
    }
}
