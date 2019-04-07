package com.blog.controllers.WebApi.Validator;

import com.blog.webUI.formTransfer.WebCreateBackupRequest;
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
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "databaseSettingsName", "error.databaseSettingsName.empty",
                "Database settings name is required");
        WebCreateBackupRequest webCreateBackupRequest = (WebCreateBackupRequest) target;
        if (webCreateBackupRequest.getBackupCreationProperties().size() == 0) {
            errors.rejectValue("backupCreationProperties", "error.backupCreationProperties.empty",
                    "Please select at least one storage to upload backup to");
        }
    }
}
