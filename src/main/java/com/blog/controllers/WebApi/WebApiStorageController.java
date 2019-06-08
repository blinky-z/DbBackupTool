package com.blog.controllers.WebApi;

import com.blog.controllers.Errors.ValidationException;
import com.blog.controllers.WebApi.Validator.WebAddStorageRequestValidator;
import com.blog.entities.storage.DropboxSettings;
import com.blog.entities.storage.LocalFileSystemSettings;
import com.blog.entities.storage.StorageSettings;
import com.blog.entities.storage.StorageType;
import com.blog.manager.StorageSettingsManager;
import com.blog.webUI.formTransfer.WebAddStorageRequest;
import com.blog.webUI.formTransfer.storage.WebDropboxSettings;
import com.blog.webUI.formTransfer.storage.WebLocalFileSystemSettings;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Optional;

/**
 * This controller is responsible for storage settings handling: creation, deletion of settings.
 */
@Controller
@RequestMapping("/storage")
public class WebApiStorageController {
    private static final Logger logger = LoggerFactory.getLogger(WebApiStorageController.class);

    private StorageSettingsManager storageSettingsManager;

    private WebAddStorageRequestValidator webAddStorageRequestValidator;

    @Autowired
    public void setStorageSettingsManager(StorageSettingsManager storageSettingsManager) {
        this.storageSettingsManager = storageSettingsManager;
    }

    @Autowired
    public void setWebAddStorageRequestValidator(WebAddStorageRequestValidator webAddStorageRequestValidator) {
        this.webAddStorageRequestValidator = webAddStorageRequestValidator;
    }

    @PostMapping
    public String createStorage(WebAddStorageRequest addStorageRequest, BindingResult bindingResult) {
        logger.info("createStorage(): Got storage settings creation request");

        webAddStorageRequestValidator.validate(addStorageRequest, bindingResult);
        if (bindingResult.hasErrors()) {
            logger.error("createStorage(): Invalid storage settings creation request. Errors: {}", bindingResult.getAllErrors());

            return "dashboard";
        }

        Optional<StorageType> storageType = StorageType.of(addStorageRequest.getStorageType());
        if (!storageType.isPresent()) {
            throw new IllegalStateException("Can't save storage settings: Invalid storage type");
        }

        StorageSettings storageSettings;

        StorageType type = storageType.get();
        switch (type) {
            case DROPBOX: {
                DropboxSettings dropboxSettings = new DropboxSettings();
                WebDropboxSettings webDropboxSettings = Objects.requireNonNull(addStorageRequest.getDropboxSettings());

                dropboxSettings.setAccessToken(webDropboxSettings.getAccessToken());

                storageSettings = StorageSettings.dropboxSettings(dropboxSettings)
                        .withSettingsName(addStorageRequest.getSettingsName())
                        .withDate(LocalDateTime.now(ZoneOffset.UTC))
                        .build();
                break;
            }
            case LOCAL_FILE_SYSTEM: {
                LocalFileSystemSettings localFileSystemSettings = new LocalFileSystemSettings();
                WebLocalFileSystemSettings webLocalFileSystemSettings = Objects.requireNonNull(
                        addStorageRequest.getLocalFileSystemSettings());

                localFileSystemSettings.setBackupPath(webLocalFileSystemSettings.getBackupPath());

                storageSettings = StorageSettings.localFileSystemSettings(localFileSystemSettings)
                        .withSettingsName(addStorageRequest.getSettingsName())
                        .withDate(LocalDateTime.now(ZoneOffset.UTC))
                        .build();
                break;
            }
            default: {
                throw new IllegalStateException("Can't save storage settings: Unknown storage type " + type);
            }
        }

        StorageSettings savedStorageSettings = storageSettingsManager.save(storageSettings);

        logger.info("Storage settings saved into database. Saved storage settings: {}", savedStorageSettings);

        return "redirect:/dashboard";
    }

    @Nullable
    private String validateDeleteStorageRequest(@Nullable String settingsName) {
        if (settingsName == null || settingsName.isEmpty() || settingsName.trim().isEmpty()) {
            return "Please, provide storage settings name to delete";
        }

        return null;
    }

    @DeleteMapping
    public String deleteStorage(@RequestParam(value = "settingsName") Optional<String> optionalSettingsName) {
        logger.info("deleteStorage(): Got storage settings deletion request");

        String error = validateDeleteStorageRequest(optionalSettingsName.orElse(null));
        if (error != null) {
            throw new ValidationException(error);
        }

        String settingsName = optionalSettingsName.get();
        storageSettingsManager.deleteById(settingsName);

        logger.info("deleteStorage(): Storage settings deleted. Settings name: {}", settingsName);

        return "redirect:/dashboard";
    }
}
