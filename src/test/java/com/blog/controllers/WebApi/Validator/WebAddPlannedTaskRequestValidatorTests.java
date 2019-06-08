package com.blog.controllers.WebApi.Validator;

import com.blog.ApplicationTests;
import com.blog.webUI.formTransfer.WebAddPlannedTaskRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class WebAddPlannedTaskRequestValidatorTests extends ApplicationTests {
    @Autowired
    private WebAddPlannedTaskRequestValidator webAddPlannedTaskRequestValidator;

    @Test
    void validate_shouldRejectFieldDatabaseSettingsName_whenMissingDatabaseSettingsName() {
        WebAddPlannedTaskRequest webAddPlannedTaskRequest = new WebAddPlannedTaskRequest();

        Errors errors = new BeanPropertyBindingResult(webAddPlannedTaskRequest, "");

        webAddPlannedTaskRequestValidator.validate(webAddPlannedTaskRequest, errors);

        assertTrue(errors.hasFieldErrors("databaseSettingsName"));
        assertEquals("error.addPlannedTaskRequest.databaseSettingsName.empty",
                errors.getFieldError("databaseSettingsName").getCode());
    }

    @Test
    void validate_shouldRejectFieldStorageSettingsNameList_whenMissingStorageSettingsNameList() {
        WebAddPlannedTaskRequest webAddPlannedTaskRequest = new WebAddPlannedTaskRequest();

        Errors errors = new BeanPropertyBindingResult(webAddPlannedTaskRequest, "");

        webAddPlannedTaskRequestValidator.validate(webAddPlannedTaskRequest, errors);

        assertTrue(errors.hasFieldErrors("storageSettingsNameList"));
        assertEquals("error.addPlannedTaskRequest.storageSettingsNameList.empty",
                errors.getFieldError("storageSettingsNameList").getCode());
    }

    @Test
    void validate_shouldRejectFieldInterval_whenMissingInterval() {
        WebAddPlannedTaskRequest webAddPlannedTaskRequest = new WebAddPlannedTaskRequest();

        Errors errors = new BeanPropertyBindingResult(webAddPlannedTaskRequest, "");

        webAddPlannedTaskRequestValidator.validate(webAddPlannedTaskRequest, errors);

        assertTrue(errors.hasFieldErrors("interval"));
        assertEquals("error.addPlannedTaskRequest.interval.empty", errors.getFieldError("interval").getCode());
    }

    @Test
    void validate_shouldRejectFieldInterval_whenIntervalIsNotNumber() {
        WebAddPlannedTaskRequest webAddPlannedTaskRequest = new WebAddPlannedTaskRequest();
        webAddPlannedTaskRequest.setInterval("132nxj3");

        Errors errors = new BeanPropertyBindingResult(webAddPlannedTaskRequest, "");

        webAddPlannedTaskRequestValidator.validate(webAddPlannedTaskRequest, errors);

        assertTrue(errors.hasFieldErrors("interval"));
        assertEquals("error.addPlannedTaskRequest.interval.malformed", errors.getFieldError("interval").getCode());
    }

    @Test
    void validate_ShouldPass_whenGivenProperDto(TestInfo testInfo) {
        WebAddPlannedTaskRequest webAddPlannedTaskRequest = new WebAddPlannedTaskRequest();
        webAddPlannedTaskRequest.setDatabaseSettingsName(testInfo.getDisplayName());
        webAddPlannedTaskRequest.setStorageSettingsNameList(Collections.singletonList(testInfo.getDisplayName()));
        webAddPlannedTaskRequest.setProcessors(Collections.emptyList());
        webAddPlannedTaskRequest.setInterval("300");

        Errors errors = new BeanPropertyBindingResult(webAddPlannedTaskRequest, "");

        webAddPlannedTaskRequestValidator.validate(webAddPlannedTaskRequest, errors);

        assertFalse(errors.hasErrors());
    }
}