package com.blog.controllers.WebApi.Validator;

import com.blog.controllers.Errors.ValidationException;
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
    void validateStorageSpecificFields(Object target, Errors errors) {
        WebAddStorageRequest addStorageRequest = (WebAddStorageRequest) target;

        Optional<StorageType> optionalStorageType = StorageType.of(addStorageRequest.getStorageType());
        if (!optionalStorageType.isPresent()) {
            errors.rejectValue("storageType", "error.addStorageRequest.storageType.malformed");
            return;
        }

        StorageType storageType = optionalStorageType.get();
        switch (storageType) {
            case DROPBOX: {
                ValidationUtils.rejectIfEmptyOrWhitespace(errors, "dropboxSettings.accessToken",
                        "error.addStorageRequest.dropboxSettings.accessToken.empty");
                break;
            }
            case LOCAL_FILE_SYSTEM: {
                ValidationUtils.rejectIfEmptyOrWhitespace(errors, "localFileSystemSettings.backupPath",
                        "error.addStorageRequest.localFileSystemSettings.backupPath.empty");
                break;
            }
            default: {
                throw new ValidationException("Unknown storage type: " + storageType);
            }
        }
    }

    public void validate(@NotNull Object target, @NotNull Errors errors) throws ValidationException {
        Objects.requireNonNull(target);
        Objects.requireNonNull(errors);

        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "storageType", "error.addStorageRequest.storageType.empty");

        // validate common fields
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "settingsName", "error.addStorageRequest.settingsName.empty");

        // validate storage specific fields
        if (!errors.hasFieldErrors("storageType")) {
            validateStorageSpecificFields(target, errors);
        }
    }
}
