package com.example.demo.controllers.WebApi.Validator;

import com.example.demo.webUI.formTransfer.WebCreateBackupRequest;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

@Component
public class WebCreateBackupRequestValidator implements Validator {
    @Override
    public boolean supports(@NotNull Class<?> clazz) {
        return WebCreateBackupRequest.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(@NotNull Object target, @NotNull Errors errors) {
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "databaseSettingsName", "error.databaseSettingsName",
                "Database settings name is required");
    }
}
