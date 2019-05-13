package com.blog.controllers.WebApi;

import com.blog.controllers.WebApi.Validator.WebCreateBackupRequestValidator;
import com.blog.entities.backup.BackupProperties;
import com.blog.entities.backup.BackupTaskState;
import com.blog.entities.backup.BackupTaskType;
import com.blog.entities.database.DatabaseSettings;
import com.blog.entities.storage.StorageSettings;
import com.blog.manager.*;
import com.blog.webUI.formTransfer.WebCreateBackupRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

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

    private ExecutorService executorService;

    private BackupTaskManager backupTaskManager;

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

    @Autowired
    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    @Autowired
    public void setBackupTaskManager(BackupTaskManager backupTaskManager) {
        this.backupTaskManager = backupTaskManager;
    }

    @PostMapping
    public String createBackup(WebCreateBackupRequest webCreateBackupRequest, BindingResult bindingResult) {
        logger.info("createBackup(): Got backup creation job");

        webCreateBackupRequestValidator.validate(webCreateBackupRequest, bindingResult);
        if (bindingResult.hasErrors()) {
            return "dashboard";
        }

        String databaseSettingsName = webCreateBackupRequest.getDatabaseSettingsName();
        DatabaseSettings databaseSettings = databaseSettingsManager.getById(databaseSettingsName).orElseThrow(() ->
                new RuntimeException(String.format("Can't retrieve database settings. Error: no database settings with name %d",
                        databaseSettingsName)));
        String databaseName = databaseSettings.getName();

        logger.info("createBackup(): Database settings: {}", databaseSettings);

        final int storagesCount = webCreateBackupRequest.getBackupCreationProperties().size();
        logger.info("createBackup(): Uploading to storages started. Total storages amount: {}", storagesCount);

        int currentStorage = 1;
        for (WebCreateBackupRequest.BackupCreationProperties backupCreationProperties :
                webCreateBackupRequest.getBackupCreationProperties().values()) {
            String storageSettingsName = backupCreationProperties.getStorageSettingsName();
            StorageSettings storageSettings = storageSettingsManager.getById(storageSettingsName).orElseThrow(() ->
                    new RuntimeException(String.format(
                            "createBackup(): Can't retrieve storage settings. Error: no storage settings with name %d",
                            storageSettingsName)));

            logger.info("createBackup(): Current storage - [{}/{}]. Storage settings: {}", currentStorage, storagesCount, storageSettings);

            List<String> processorList = backupCreationProperties.getProcessors();
            BackupProperties backupProperties =
                    backupLoadManager.initNewBackupProperties(storageSettings, processorList, databaseName);

            Integer taskId = backupTaskManager.initNewTask(BackupTaskType.CREATE_BACKUP, backupProperties);
            Future task = executorService.submit(
                    new Runnable() {
                        @Override
                        public void run() {
                            try {
                                backupTaskManager.updateTaskState(taskId, BackupTaskState.CREATING);

                                logger.info("createBackup(): Creating backup...");
                                InputStream backupStream = databaseBackupManager.createBackup(databaseSettings, taskId);

                                backupTaskManager.updateTaskState(taskId, BackupTaskState.APPLYING_PROCESSORS);

                                logger.info("createBackup(): Applying processors on created backup. Processors: {}", processorList);
                                InputStream processedBackupStream = backupProcessorManager.process(backupStream, processorList);

                                logger.info("createBackup(): Uploading backup...");

                                backupTaskManager.updateTaskState(taskId, BackupTaskState.UPLOADING);

                                backupLoadManager.uploadBackup(processedBackupStream, backupProperties);

                                backupTaskManager.updateTaskState(taskId, BackupTaskState.COMPLETED);

                                logger.info("createBackup(): Creating backup completed. Backup properties: {}", backupProperties);
                            } catch (RuntimeException ex) {
                                logger.info("createBackup(): Error occurred while creating backup. Backup properties: {}",
                                        backupProperties, ex);
                                backupTaskManager.setError(taskId);
                            }
                        }
                    }
            );
            backupTaskManager.addTaskFuture(taskId, task);

            currentStorage++;
        }

        return "redirect:/dashboard";
    }
}
