package com.blog.controllers.WebApi.Validator;

import com.blog.controllers.Errors.ValidationError;
import com.blog.entities.storage.StorageType;
import com.blog.webUI.formTransfer.WebAddStorageRequest;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;

import java.util.Objects;
import java.util.Optional;

/**
 * Validates storage settings creation request
 *
 * @see com.blog.controllers.WebApi.WebApiStorageController#createStorage(WebAddStorageRequest, BindingResult)
 */
@Component
public class WebAddStorageRequestValidator {
    public void validate(@NotNull Object target, @NotNull Errors errors) {
        Objects.requireNonNull(target);
        Objects.requireNonNull(errors);

        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "storageType",
                "error.addStorageRequest.storageType.empty", "Please specify storage type");

        WebAddStorageRequest addStorageRequest = (WebAddStorageRequest) target;

        // validate common fields
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "settingsName",
                "error.addStorageRequest.settingsName.empty", "Settings name must not be empty");

        // validate storage specific fields
        if (!errors.hasFieldErrors("storageType")) {
            Optional<StorageType> optionalStorageType = StorageType.of(addStorageRequest.getStorageType());
            if (!optionalStorageType.isPresent()) {
                errors.rejectValue("storageType", "error.addStorageRequest.storageType.malformed",
                        "Invalid storage type");
                return;
            }

            StorageType storageType = optionalStorageType.get();
            switch (storageType) {
                case DROPBOX: {
                    ValidationUtils.rejectIfEmptyOrWhitespace(errors, "dropboxSettings.accessToken",
                            "error.addStorageRequest.dropboxSettings.accessToken.empty",
                            "Access token must not be empty");
                    break;
                }
                case LOCAL_FILE_SYSTEM: {
                    ValidationUtils.rejectIfEmptyOrWhitespace(errors, "localFileSystemSettings.backupPath",
                            "error.addStorageRequest.localFileSystemSettings.backupPath.empty",
                            "Backup path must not be empty");
                    break;
                }
                default: {
                    throw new ValidationError("Can't validate storage settings. Unknown storage type: " + storageType);
                }
            }
        }
    }
}
