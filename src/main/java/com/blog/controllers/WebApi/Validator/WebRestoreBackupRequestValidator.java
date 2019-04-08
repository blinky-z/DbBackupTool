package com.blog.controllers.WebApi.Validator;

import com.blog.webUI.formTransfer.WebRestoreBackupRequest;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;

@Component
public class WebRestoreBackupRequestValidator {
    public void validate(@NotNull Object target, @NotNull Errors errors) {
        WebRestoreBackupRequest webRestoreBackupRequest = (WebRestoreBackupRequest) target;

        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "backupId", "error.createBackupRequest.backupId.empty",
                "Please provide backup to restore");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "databaseSettingsName",
                "error.createBackupRequest.databaseSettingsName.empty",
                "Please provide database to restore backup to");

        String backupIdAsString = webRestoreBackupRequest.getBackupId();
        if (backupIdAsString != null && !backupIdAsString.isEmpty()) {
            try {
                Integer.valueOf(backupIdAsString);
            } catch (NumberFormatException ex) {
                errors.rejectValue("backupId", "error.createBackupRequest.backupId.malformed",
                        "Invalid backup id. Must be only number");
            }
        }
    }
}

