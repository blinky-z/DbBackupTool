package com.blog.controllers.WebApi.Validator;

import com.blog.webUI.formTransfer.WebRestoreBackupRequest;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;

import java.util.Objects;


/**
 * Validates backup restoration request
 *
 * @see com.blog.controllers.WebApi.WebApiBackupController#restoreBackup(WebRestoreBackupRequest, BindingResult)
 */
@Component
public class WebRestoreBackupRequestValidator {
    public void validate(@NotNull Object target, @NotNull Errors errors) {
        Objects.requireNonNull(target);
        Objects.requireNonNull(errors);

        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "backupId",
                "error.restoreBackupRequest.backupId.empty");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "databaseSettingsName",
                "error.restoreBackupRequest.databaseSettingsName.empty");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "storageSettingsName",
                "error.restoreBackupRequest.storageSettingsName.empty");

        if (!errors.hasFieldErrors("backupId")) {
            try {
                WebRestoreBackupRequest webRestoreBackupRequest = (WebRestoreBackupRequest) target;
                Integer.valueOf(webRestoreBackupRequest.getBackupId());
            } catch (NumberFormatException ex) {
                errors.rejectValue("backupId", "error.restoreBackupRequest.backupId.malformed");
            }
        }
    }
}

