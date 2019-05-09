package com.blog.controllers.WebApi.Validator;

import com.blog.webUI.formTransfer.WebDeleteBackupRequest;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;

@Component
public class WebDeleteBackupRequestValidator {
    public void validate(@NotNull Object target, @NotNull Errors errors) {
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "backupId", "error.deleteBackupRequest.backupId.empty",
                "Backup ID is required");
        WebDeleteBackupRequest webDeleteBackupRequest = (WebDeleteBackupRequest) target;

        String backupId = webDeleteBackupRequest.getBackupId();
        if (backupId != null && !backupId.isEmpty()) {
            try {
                Integer.valueOf(backupId);
            } catch (NumberFormatException ex) {
                errors.rejectValue("backupId", "error.deleteBackupRequest.backupId.malformed",
                        "Invalid backup ID. Must be only number");
            }
        }
    }
}
