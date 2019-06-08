package com.blog.controllers.WebApi.Validator;

import com.blog.ApplicationTests;
import com.blog.controllers.Errors.ValidationException;
import com.blog.entities.database.DatabaseType;
import com.blog.webUI.formTransfer.WebAddDatabaseRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import static org.junit.jupiter.api.Assertions.*;

class WebAddDatabaseRequestValidatorTests extends ApplicationTests {
    @Autowired
    WebAddDatabaseRequestValidator webAddDatabaseRequestValidator;

    @Test
    void validate_ShouldRejectDatabaseTypeField_whenMissingDatabaseType() {
        WebAddDatabaseRequest webAddDatabaseRequest = new WebAddDatabaseRequest();

        Errors errors = new BeanPropertyBindingResult(webAddDatabaseRequest, "");

        webAddDatabaseRequestValidator.validate(webAddDatabaseRequest, errors);

        assertTrue(errors.hasFieldErrors("databaseType"));
        assertEquals("error.addDatabaseRequest.databaseType.empty", errors.getFieldError("databaseType").getCode());
    }

    @Test
    void validate_ShouldRejectDatabaseTypeField_whenPassedDatabaseTypeIsMalformed() {
        WebAddDatabaseRequest webAddDatabaseRequest = new WebAddDatabaseRequest();
        webAddDatabaseRequest.setDatabaseType("db113f");

        Errors errors = new BeanPropertyBindingResult(webAddDatabaseRequest, "");

        webAddDatabaseRequestValidator.validate(webAddDatabaseRequest, errors);

        assertTrue(errors.hasFieldErrors("databaseType"));
        assertEquals("error.addDatabaseRequest.databaseType.malformed", errors.getFieldError("databaseType").getCode());
    }

    @Test
    void validate_ShouldRejectSettingsNameField_whenMissingSettingsName() {
        WebAddDatabaseRequest webAddDatabaseRequest = new WebAddDatabaseRequest();

        Errors errors = new BeanPropertyBindingResult(webAddDatabaseRequest, "");

        webAddDatabaseRequestValidator.validate(webAddDatabaseRequest, errors);

        assertTrue(errors.hasFieldErrors("settingsName"));
        assertEquals("error.addDatabaseRequest.settingsName.empty", errors.getFieldError("settingsName").getCode());
    }

    @Test
    void validate_ShouldRejectHostField_whenMissingHost() {
        WebAddDatabaseRequest webAddDatabaseRequest = new WebAddDatabaseRequest();

        Errors errors = new BeanPropertyBindingResult(webAddDatabaseRequest, "");

        webAddDatabaseRequestValidator.validate(webAddDatabaseRequest, errors);

        assertTrue(errors.hasFieldErrors("host"));
        assertEquals("error.addDatabaseRequest.host.empty", errors.getFieldError("host").getCode());
    }

    @Test
    void validate_ShouldRejectPortField_whenMissingPort() {
        WebAddDatabaseRequest webAddDatabaseRequest = new WebAddDatabaseRequest();

        Errors errors = new BeanPropertyBindingResult(webAddDatabaseRequest, "");

        webAddDatabaseRequestValidator.validate(webAddDatabaseRequest, errors);

        assertTrue(errors.hasFieldErrors("port"));
        assertEquals("error.addDatabaseRequest.port.empty", errors.getFieldError("port").getCode());
    }

    @Test
    void validate_ShouldRejectPortField_whenPassedPortIsMalformed() {
        WebAddDatabaseRequest webAddDatabaseRequest = new WebAddDatabaseRequest();
        webAddDatabaseRequest.setPort("8080f");

        Errors errors = new BeanPropertyBindingResult(webAddDatabaseRequest, "");

        webAddDatabaseRequestValidator.validate(webAddDatabaseRequest, errors);

        assertTrue(errors.hasFieldErrors("port"));
        assertEquals("error.addDatabaseRequest.port.malformed", errors.getFieldError("port").getCode());
    }

    @Test
    void validate_ShouldRejectDatabaseNameField_whenMissingDatabaseName() {
        WebAddDatabaseRequest webAddDatabaseRequest = new WebAddDatabaseRequest();

        Errors errors = new BeanPropertyBindingResult(webAddDatabaseRequest, "");

        webAddDatabaseRequestValidator.validate(webAddDatabaseRequest, errors);

        assertTrue(errors.hasFieldErrors("databaseName"));
        assertEquals("error.addDatabaseRequest.databaseName.empty", errors.getFieldError("databaseName").getCode());
    }

    @Test
    void validate_ShouldRejectLoginField_whenMissingLogin() {
        WebAddDatabaseRequest webAddDatabaseRequest = new WebAddDatabaseRequest();

        Errors errors = new BeanPropertyBindingResult(webAddDatabaseRequest, "");

        webAddDatabaseRequestValidator.validate(webAddDatabaseRequest, errors);

        assertTrue(errors.hasFieldErrors("login"));
        assertEquals("error.addDatabaseRequest.login.empty", errors.getFieldError("login").getCode());
    }

    @Test
    void validate_ShouldRejectPasswordField_whenMissingPassword() {
        WebAddDatabaseRequest webAddDatabaseRequest = new WebAddDatabaseRequest();

        Errors errors = new BeanPropertyBindingResult(webAddDatabaseRequest, "");

        webAddDatabaseRequestValidator.validate(webAddDatabaseRequest, errors);

        assertTrue(errors.hasFieldErrors("password"));
        assertEquals("error.addDatabaseRequest.password.empty", errors.getFieldError("password").getCode());
    }

    @Test
    void validate_ShouldPass_whenGivenProperPostgresSettings() {
        WebAddDatabaseRequest webAddDatabaseRequest = new WebAddDatabaseRequest();
        webAddDatabaseRequest.setDatabaseType("postgres");
        webAddDatabaseRequest.setSettingsName("testPostgresSettings");
        webAddDatabaseRequest.setHost("localhost");
        webAddDatabaseRequest.setPort("5432");
        webAddDatabaseRequest.setDatabaseName("postgres");
        webAddDatabaseRequest.setLogin("postgres");
        webAddDatabaseRequest.setPassword("postgres");

        Errors errors = new BeanPropertyBindingResult(webAddDatabaseRequest, "");

        webAddDatabaseRequestValidator.validate(webAddDatabaseRequest, errors);

        assertFalse(errors.hasErrors());
    }

    @Test
    void validate_shouldNotThrowValidationException_whenPassAnyDatabaseType(TestInfo testInfo) {
        for (DatabaseType database : DatabaseType.values()) {
            WebAddDatabaseRequest webAddDatabaseRequest = new WebAddDatabaseRequest();
            webAddDatabaseRequest.setSettingsName(testInfo.getDisplayName());
            webAddDatabaseRequest.setDatabaseType(database.getDatabaseAsString());

            Errors errors = new BeanPropertyBindingResult(webAddDatabaseRequest, "");

            try {
                webAddDatabaseRequestValidator.validate(webAddDatabaseRequest, errors);
            } catch (ValidationException ex) {
                fail("Exception must not be thrown on database type " + database);
            }
        }
    }
}
