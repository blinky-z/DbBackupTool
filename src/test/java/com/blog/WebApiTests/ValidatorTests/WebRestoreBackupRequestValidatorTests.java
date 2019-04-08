package com.blog.WebApiTests.ValidatorTests;

import com.blog.ApplicationTests;
import com.blog.controllers.WebApi.Validator.WebRestoreBackupRequestValidator;
import com.blog.webUI.formTransfer.WebRestoreBackupRequest;
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
public class WebRestoreBackupRequestValidatorTests extends ApplicationTests {
    @Autowired
    private WebRestoreBackupRequestValidator webRestoreBackupRequestValidator;

    @Test
    public void validate_shouldRejectBackupIfField_whenMissingBackupId() {
        WebRestoreBackupRequest webRestoreBackupRequest = new WebRestoreBackupRequest();

        Errors errors = new BeanPropertyBindingResult(webRestoreBackupRequest, "");

        webRestoreBackupRequestValidator.validate(webRestoreBackupRequest, errors);

        assertTrue(errors.hasFieldErrors("backupId"));
    }

    @Test
    public void validate_shouldRejectBackupIfField_whenBackupIdMalformed() {
        WebRestoreBackupRequest webRestoreBackupRequest = new WebRestoreBackupRequest();
        webRestoreBackupRequest.setBackupId("134f");

        Errors errors = new BeanPropertyBindingResult(webRestoreBackupRequest, "");

        webRestoreBackupRequestValidator.validate(webRestoreBackupRequest, errors);

        assertTrue(errors.hasFieldErrors("backupId"));
    }

    @Test
    public void validate_shouldRejectDatabaseSettingsName_whenMissingDatabaseSettingsName() {
        WebRestoreBackupRequest webRestoreBackupRequest = new WebRestoreBackupRequest();

        Errors errors = new BeanPropertyBindingResult(webRestoreBackupRequest, "");

        webRestoreBackupRequestValidator.validate(webRestoreBackupRequest, errors);

        assertTrue(errors.hasFieldErrors("databaseSettingsName"));
    }
}