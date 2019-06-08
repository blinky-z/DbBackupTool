package com.blog.controllers.WebApi.Validator;

import com.blog.webUI.formTransfer.WebCreateBackupRequest;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;

import java.util.Objects;

/**
 * Validates backup creation request
 *
 * @see com.blog.controllers.WebApi.WebApiBackupController#createBackup(WebCreateBackupRequest, BindingResult)
 */
@Component
public class WebCreateBackupRequestValidator {
    public void validate(@NotNull Object target, @NotNull Errors errors) {
        Objects.requireNonNull(target);
        Objects.requireNonNull(errors);

        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "databaseSettingsName",
                "error.createBackupRequest.databaseSettingsName.empty");

        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "storageSettingsNameList",
                "error.createBackupRequest.storageSettingsNameList.empty");
    }
}
