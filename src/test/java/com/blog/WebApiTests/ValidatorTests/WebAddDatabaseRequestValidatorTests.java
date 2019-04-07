package com.blog.WebApiTests.ValidatorTests;

import com.blog.controllers.WebApi.Validator.WebAddDatabaseRequestValidator;
import com.blog.ApplicationTests;
import com.blog.webUI.formTransfer.WebAddDatabaseRequest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest
public class WebAddDatabaseRequestValidatorTests extends ApplicationTests {
    @Autowired
    WebAddDatabaseRequestValidator webAddDatabaseRequestValidator;

    /**
     * Construct WebAddDatabaseRequest object with minimum required fields set to test common fields
     *
     * @return new WebAddDatabaseRequest object to use in validator
     */
    private WebAddDatabaseRequest getWebAddDatabaseRequestForTestingCommonFields() {
        WebAddDatabaseRequest webAddDatabaseRequest = new WebAddDatabaseRequest();
        webAddDatabaseRequest.setDatabaseType("postgres");
        return webAddDatabaseRequest;
    }

    @Test
    public void validate_ShouldRejectDatabaseTypeField_whenMissingDatabaseType() {
        WebAddDatabaseRequest webAddDatabaseRequest = new WebAddDatabaseRequest();

        Errors errors = new BeanPropertyBindingResult(webAddDatabaseRequest, "");

        webAddDatabaseRequestValidator.validate(webAddDatabaseRequest, errors);

        assertTrue(errors.hasFieldErrors("databaseType"));
    }

    @Test
    public void validate_ShouldRejectDatabaseTypeField_whenSetInvalidDatabaseType() {
        WebAddDatabaseRequest webAddDatabaseRequest = new WebAddDatabaseRequest();
        webAddDatabaseRequest.setDatabaseType("invalidDatabaseType");

        Errors errors = new BeanPropertyBindingResult(webAddDatabaseRequest, "");

        webAddDatabaseRequestValidator.validate(webAddDatabaseRequest, errors);

        assertTrue(errors.hasFieldErrors("databaseType"));
    }

    @Test
    public void validate_ShouldRejectSettingsNameField_whenMissingSettingsName() {
        WebAddDatabaseRequest webAddDatabaseRequest = getWebAddDatabaseRequestForTestingCommonFields();

        Errors errors = new BeanPropertyBindingResult(webAddDatabaseRequest, "");

        webAddDatabaseRequestValidator.validate(webAddDatabaseRequest, errors);

        assertTrue(errors.hasFieldErrors("settingsName"));
    }

    @Test
    public void validate_ShouldRejectHostField_whenMissingHost() {
        WebAddDatabaseRequest webAddDatabaseRequest = getWebAddDatabaseRequestForTestingCommonFields();

        Errors errors = new BeanPropertyBindingResult(webAddDatabaseRequest, "");

        webAddDatabaseRequestValidator.validate(webAddDatabaseRequest, errors);

        assertTrue(errors.hasFieldErrors("host"));
    }

    @Test
    public void validate_ShouldRejectPortField_whenMissingPort() {
        WebAddDatabaseRequest webAddDatabaseRequest = getWebAddDatabaseRequestForTestingCommonFields();

        Errors errors = new BeanPropertyBindingResult(webAddDatabaseRequest, "");

        webAddDatabaseRequestValidator.validate(webAddDatabaseRequest, errors);

        assertTrue(errors.hasFieldErrors("port"));
    }

    @Test
    public void validate_ShouldRejectPortField_whenPortMalformed() {
        WebAddDatabaseRequest webAddDatabaseRequest = getWebAddDatabaseRequestForTestingCommonFields();
        webAddDatabaseRequest.setPort("8080f");

        Errors errors = new BeanPropertyBindingResult(webAddDatabaseRequest, "");

        webAddDatabaseRequestValidator.validate(webAddDatabaseRequest, errors);

        assertTrue(errors.hasFieldErrors("port"));
    }

    @Test
    public void validate_ShouldRejectDatabaseNameField_whenMissingDatabaseName() {
        WebAddDatabaseRequest webAddDatabaseRequest = getWebAddDatabaseRequestForTestingCommonFields();

        Errors errors = new BeanPropertyBindingResult(webAddDatabaseRequest, "");

        webAddDatabaseRequestValidator.validate(webAddDatabaseRequest, errors);

        assertTrue(errors.hasFieldErrors("databaseName"));
    }

    @Test
    public void validate_ShouldRejectLoginField_whenMissingLogin() {
        WebAddDatabaseRequest webAddDatabaseRequest = getWebAddDatabaseRequestForTestingCommonFields();

        Errors errors = new BeanPropertyBindingResult(webAddDatabaseRequest, "");

        webAddDatabaseRequestValidator.validate(webAddDatabaseRequest, errors);

        assertTrue(errors.hasFieldErrors("login"));
    }

    @Test
    public void validate_ShouldRejectPasswordField_whenMissingPassword() {
        WebAddDatabaseRequest webAddDatabaseRequest = getWebAddDatabaseRequestForTestingCommonFields();

        Errors errors = new BeanPropertyBindingResult(webAddDatabaseRequest, "");

        webAddDatabaseRequestValidator.validate(webAddDatabaseRequest, errors);

        assertTrue(errors.hasFieldErrors("password"));
    }

    @Test
    public void validate_ShouldPass_whenGivenProperPostgresDatabaseSettings() {
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

        System.out.println(errors.getAllErrors());
        assertFalse(errors.hasErrors());
    }
}
