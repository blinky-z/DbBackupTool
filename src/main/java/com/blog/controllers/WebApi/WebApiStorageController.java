package com.blog.controllers.WebApi;

import com.blog.controllers.Errors.DataAccessUserError;
import com.blog.controllers.Errors.ValidationError;
import com.blog.controllers.WebApi.Validator.WebAddStorageRequestValidator;
import com.blog.entities.storage.DropboxSettings;
import com.blog.entities.storage.LocalFileSystemSettings;
import com.blog.entities.storage.StorageSettings;
import com.blog.entities.storage.StorageType;
import com.blog.manager.StorageSettingsManager;
import com.blog.webUI.formTransfer.WebAddStorageRequest;
import com.blog.webUI.formTransfer.storage.WebDropboxSettings;
import com.blog.webUI.formTransfer.storage.WebLocalFileSystemSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.NonTransientDataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Objects;
import java.util.Optional;

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

    private String validateDeleteStorageRequest(Optional<String> optionalSettingsName) {
        if (!optionalSettingsName.isPresent()) {
            return "Please, provide storage settings name to delete";
        }

        String settingsName = optionalSettingsName.get();
        if (settingsName.isEmpty()) {
            return "Please, provide storage settings name to delete";
        }

        return "";
    }

    @DeleteMapping
    public String deleteStorage(@RequestParam(value = "settingsName") Optional<String> optionalSettingsName) {
        String error = validateDeleteStorageRequest(optionalSettingsName);
        if (!error.isEmpty()) {
            throw new ValidationError(error);
        }

        String settingsName = optionalSettingsName.get();

        logger.info("deleteStorage(): Got storage settings deletion job. Settings name: {}", settingsName);

        try {
            storageSettingsManager.deleteById(settingsName);
        } catch (NonTransientDataAccessException ignored) {

        }

        return "redirect:/dashboard";
    }

    @PostMapping
    public String createStorage(WebAddStorageRequest addStorageRequest, BindingResult bindingResult) {
        logger.info("createStorage(): Got storage creation job");

        webAddStorageRequestValidator.validate(addStorageRequest, bindingResult);
        if (bindingResult.hasErrors()) {
            return "dashboard";
        }

        String settingsName = addStorageRequest.getSettingsName();
        if (storageSettingsManager.existsById(settingsName)) {
            throw new DataAccessUserError("Storage settings with name '" + settingsName + "' already exists");
        }

        Optional<StorageType> storageType = StorageType.of(addStorageRequest.getStorageType());
        if (storageType.isPresent()) {
            switch (storageType.get()) {
                case DROPBOX: {
                    DropboxSettings dropboxSettings = new DropboxSettings();
                    WebDropboxSettings webDropboxSettings = Objects.requireNonNull(addStorageRequest.getDropboxSettings());

                    dropboxSettings.setAccessToken(webDropboxSettings.getAccessToken());

                    StorageSettings storageSettings = StorageSettings.dropboxSettings(dropboxSettings)
                            .withSettingsName(addStorageRequest.getSettingsName())
                            .build();
                    storageSettingsManager.save(storageSettings);
                    break;
                }
                case LOCAL_FILE_SYSTEM: {
                    LocalFileSystemSettings localFileSystemSettings = new LocalFileSystemSettings();
                    WebLocalFileSystemSettings webLocalFileSystemSettings = Objects.requireNonNull(
                            addStorageRequest.getLocalFileSystemSettings());

                    localFileSystemSettings.setBackupPath(webLocalFileSystemSettings.getBackupPath());

                    StorageSettings storageSettings = StorageSettings.localFileSystemSettings(localFileSystemSettings)
                            .withSettingsName(addStorageRequest.getSettingsName())
                            .build();
                    storageSettingsManager.save(storageSettings);
                    break;
                }
            }
        } else {
            throw new RuntimeException("Can't create storage configuration. Error: Unknown storage type");
        }

        return "redirect:/dashboard";
    }
}
