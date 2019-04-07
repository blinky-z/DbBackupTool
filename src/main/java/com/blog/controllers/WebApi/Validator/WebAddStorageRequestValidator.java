package com.blog.controllers.WebApi.Validator;

import com.blog.entities.storage.StorageType;
import com.blog.webUI.formTransfer.WebAddStorageRequest;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import java.util.Optional;

@Component
public class WebAddStorageRequestValidator implements Validator {
    @Override
    public boolean supports(@NotNull Class<?> clazz) {
        return WebAddStorageRequest.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(@NotNull Object target, @NotNull Errors errors) {
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "storageType", "error.addStorageRequest.storageType.empty",
                "Please specify storage type");

        WebAddStorageRequest addStorageRequest = (WebAddStorageRequest) target;

        Optional<StorageType> optionalStorageType = StorageType.of(addStorageRequest.getStorageType());
        if (optionalStorageType.isPresent()) {
            ValidationUtils.rejectIfEmptyOrWhitespace(errors, "settingsName", "error.addStorageRequest.settingsName.empty",
                    "Please provide settings name");

            StorageType storageType = optionalStorageType.get();
            switch (storageType) {
                case DROPBOX: {
                    ValidationUtils.rejectIfEmptyOrWhitespace(errors, "dropboxSettings.accessToken",
                            "error.addStorageRequest.dropboxSettings.accessToken.empty",
                            "Please provide access token");
                    break;
                }
                case LOCAL_FILE_SYSTEM: {
                    ValidationUtils.rejectIfEmptyOrWhitespace(errors, "localFileSystemSettings.backupPath",
                            "error.addStorageRequest.localFileSystemSettings.backupPath.empty",
                            "Please provide backup path");
                    break;
                }
            }
        } else {
            errors.rejectValue("storageType", "error.addStorageRequest.storageType.malformed",
                    "Please provide proper storage type");
        }
    }
}
