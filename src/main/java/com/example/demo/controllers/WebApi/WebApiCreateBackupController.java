package com.example.demo.controllers.WebApi;

import com.example.demo.controllers.WebApi.Errors.ValidationError;
import com.example.demo.controllers.WebApi.Validator.WebCreateBackupRequestValidator;
import com.example.demo.entities.database.DatabaseSettings;
import com.example.demo.entities.storage.StorageSettings;
import com.example.demo.manager.*;
import com.example.demo.webUI.formTransfer.WebCreateBackupRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.validation.Valid;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/create-backup")
public class WebApiCreateBackupController {
    private static final Logger logger = LoggerFactory.getLogger(WebApiCreateBackupController.class);

    private DatabaseBackupManager databaseBackupManager;

    private DatabaseSettingsManager databaseSettingsManager;

    private StorageSettingsManager storageSettingsManager;

    private BackupLoadManager backupLoadManager;

    private BackupProcessorManager backupProcessorManager;

    private WebCreateBackupRequestValidator webCreateBackupRequestValidator;

    @Autowired
    public void setDatabaseBackupManager(DatabaseBackupManager databaseBackupManager) {
        this.databaseBackupManager = databaseBackupManager;
    }

    @Autowired
    public void setDatabaseSettingsManager(DatabaseSettingsManager databaseSettingsManager) {
        this.databaseSettingsManager = databaseSettingsManager;
    }

    @Autowired
    public void setStorageSettingsManager(StorageSettingsManager storageSettingsManager) {
        this.storageSettingsManager = storageSettingsManager;
    }

    @Autowired
    public void setBackupLoadManager(BackupLoadManager backupLoadManager) {
        this.backupLoadManager = backupLoadManager;
    }

    @Autowired
    public void setBackupProcessorManager(BackupProcessorManager backupProcessorManager) {
        this.backupProcessorManager = backupProcessorManager;
    }

    @Autowired
    public void setWebCreateBackupRequestValidator(WebCreateBackupRequestValidator webCreateBackupRequestValidator) {
        this.webCreateBackupRequestValidator = webCreateBackupRequestValidator;
    }

    private String validateCreateBackupRequest(WebCreateBackupRequest createBackupRequest) {
        if (createBackupRequest.getDatabaseSettingsName() == null) {
            return "Please, provide database to backup";
        }

        HashMap<String, WebCreateBackupRequest.BackupCreationProperties> backupCreationProperties =
                createBackupRequest.getBackupCreationProperties();
        if (backupCreationProperties.size() == 0) {
            return "Please, select at least one storage to upload backup to";
        }

        for (Map.Entry<String, WebCreateBackupRequest.BackupCreationProperties> entry : backupCreationProperties.entrySet()) {
            WebCreateBackupRequest.BackupCreationProperties currentBackupCreationProperties = entry.getValue();
            if (currentBackupCreationProperties == null) {
                return "Missing storage settings. Settings name: " + entry.getKey();
            }
        }

        return "";
    }

    @PostMapping
    public String createBackup(WebCreateBackupRequest createBackupRequest, BindingResult bindingResult) {
        logger.info("createBackup(): Got backup creation job");

//        String error = validateCreateBackupRequest(createBackupRequest);
//        if (!error.isEmpty()) {
//            throw new ValidationError(error);
//        }

        webCreateBackupRequestValidator.validate(createBackupRequest, bindingResult);
        if (bindingResult.hasErrors()) {
            logger.info("Has errors: {}", bindingResult.getAllErrors());
            return "dashboard";
        }

        String databaseSettingsName = createBackupRequest.getDatabaseSettingsName();
        DatabaseSettings databaseSettings = databaseSettingsManager.getById(databaseSettingsName).orElseThrow(() ->
                new RuntimeException(
                        String.format("Can't retrieve database settings. Error: no database settings with name %d", databaseSettingsName)));
        String databaseName = databaseSettings.getName();

        logger.info("createBackup(): Database settings: {}", databaseSettings);

        for (WebCreateBackupRequest.BackupCreationProperties backupCreationProperties :
                createBackupRequest.getBackupCreationProperties().values()) {
            String storageSettingsName = backupCreationProperties.getStorageSettingsName();
            StorageSettings storageSettings = storageSettingsManager.getById(storageSettingsName).orElseThrow(() ->
                    new RuntimeException(
                            String.format("createBackup(): Can't retrieve storage settings. Error: no storage settings with name %d",
                                    storageSettingsName)));

            logger.info("Current storage settings: {}", storageSettings.toString());

            logger.info("createBackup(): Creating backup...");
            InputStream backupStream = databaseBackupManager.createBackup(databaseSettings);

            List<String> processorList = backupCreationProperties.getProcessors();
            logger.info("createBackup(): Applying processors on created backup. Processors: {}", processorList);
            InputStream processedBackupStream = backupProcessorManager.process(backupStream, processorList);

            logger.info("createBackup(): Uploading backup...");
            backupLoadManager.uploadBackup(processedBackupStream, storageSettings, processorList, databaseName);
        }

        return "redirect:/dashboard";
    }
}
