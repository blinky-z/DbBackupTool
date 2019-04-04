package com.example.demo.controllers.WebApi;

import com.example.demo.controllers.WebApi.Errors.ValidationError;
import com.example.demo.entities.storage.DropboxSettings;
import com.example.demo.entities.storage.LocalFileSystemSettings;
import com.example.demo.entities.storage.StorageSettings;
import com.example.demo.entities.storage.StorageType;
import com.example.demo.manager.StorageSettingsManager;
import com.example.demo.webUI.formTransfer.WebAddStorageRequest;
import com.example.demo.webUI.formTransfer.storage.WebDropboxSettings;
import com.example.demo.webUI.formTransfer.storage.WebLocalFileSystemSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
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

    @Autowired
    public void setStorageSettingsManager(StorageSettingsManager storageSettingsManager) {
        this.storageSettingsManager = storageSettingsManager;
    }

    @DeleteMapping
    public String deleteStorage(@RequestParam(value = "id") Optional<Integer> optionalId) {
        if (!optionalId.isPresent()) {
            throw new ValidationError("Please, provide storage ID to delete");
        }

        Integer id = optionalId.get();

        logger.info("deleteStorage(): Got storage deletion job. Storage ID: {}", id);

        storageSettingsManager.deleteById(id);

        return "redirect:/dashboard";
    }

    private String validateAddStorageRequest(WebAddStorageRequest addStorageRequest) {
        String storageTypeAsString = addStorageRequest.getStorageType();
        if (storageTypeAsString == null || storageTypeAsString.isEmpty()) {
            return "Please, specify storage type";
        }
        Optional<StorageType> optionalStorageType = StorageType.of(storageTypeAsString);
        if (!optionalStorageType.isPresent()) {
            return "Please, provide proper storage type";
        }
        StorageType storageType = optionalStorageType.get();
        switch (storageType) {
            case DROPBOX: {
                if (addStorageRequest.getDropboxSettings() == null) {
                    return "Dropbox storage was specified, but no settings provided";
                }
                WebDropboxSettings webDropboxSettings = addStorageRequest.getDropboxSettings();
                String accessToken = webDropboxSettings.getAccessToken();
                if (accessToken == null || accessToken.isEmpty()) {
                    return "Invalid Dropbox settings";
                }
                break;
            }
            case LOCAL_FILE_SYSTEM: {
                if (addStorageRequest.getDropboxSettings() == null) {
                    return "Local File System storage was specified, but no settings provided";
                }
                WebLocalFileSystemSettings webLocalFileSystemSettings = addStorageRequest.getLocalFileSystemSettings();
                String backupPath = webLocalFileSystemSettings.getBackupPath();
                if (backupPath == null || backupPath.isEmpty()) {
                    return "Invalid Local File System settings";
                }
                break;
            }
        }
        return "";
    }

    @PostMapping
    public String createStorage(WebAddStorageRequest addStorageRequest) {
        logger.info("createStorage(): Got storage creation job");

        String error = validateAddStorageRequest(addStorageRequest);
        if (!error.isEmpty()) {
            throw new ValidationError(error);
        }

        Optional<StorageType> storageType = StorageType.of(addStorageRequest.getStorageType());
        if (storageType.isPresent()) {
            switch (storageType.get()) {
                case DROPBOX: {
                    DropboxSettings dropboxSettings = new DropboxSettings();
                    WebDropboxSettings webDropboxSettings = Objects.requireNonNull(addStorageRequest.getDropboxSettings());

                    dropboxSettings.setAccessToken(webDropboxSettings.getAccessToken());

                    StorageSettings storageSettings = StorageSettings.dropboxSettings(dropboxSettings).build();
                    storageSettingsManager.save(storageSettings);
                    break;
                }
                case LOCAL_FILE_SYSTEM: {
                    LocalFileSystemSettings localFileSystemSettings = new LocalFileSystemSettings();
                    WebLocalFileSystemSettings webLocalFileSystemSettings = Objects.requireNonNull(
                            addStorageRequest.getLocalFileSystemSettings());

                    localFileSystemSettings.setBackupPath(webLocalFileSystemSettings.getBackupPath());

                    StorageSettings storageSettings = StorageSettings.localFileSystemSettings(localFileSystemSettings).build();
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
