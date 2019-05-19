package com.blog.controllers.WebApi;

import com.blog.controllers.Errors.ValidationError;
import com.blog.controllers.WebApi.Validator.WebCreateBackupRequestValidator;
import com.blog.controllers.WebApi.Validator.WebRestoreBackupRequestValidator;
import com.blog.entities.backup.BackupProperties;
import com.blog.entities.backup.BackupTask;
import com.blog.entities.database.DatabaseSettings;
import com.blog.entities.storage.StorageSettings;
import com.blog.manager.*;
import com.blog.webUI.formTransfer.WebCreateBackupRequest;
import com.blog.webUI.formTransfer.WebDeleteBackupRequest;
import com.blog.webUI.formTransfer.WebRestoreBackupRequest;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;


/**
 * This controller is responsible for backup handling: creation, restoration and deletion of backup.
 */
@Controller
public class WebApiBackupController {
    private static final Logger logger = LoggerFactory.getLogger(WebApiBackupController.class);

    private DatabaseBackupManager databaseBackupManager;

    private DatabaseSettingsManager databaseSettingsManager;

    private StorageSettingsManager storageSettingsManager;

    private BackupLoadManager backupLoadManager;

    private BackupProcessorManager backupProcessorManager;

    private WebCreateBackupRequestValidator webCreateBackupRequestValidator;

    private WebRestoreBackupRequestValidator webRestoreBackupRequestValidator;

    private ExecutorService executorService;

    private BackupTaskManager backupTaskManager;

    private BackupPropertiesManager backupPropertiesManager;

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
    public void setWebRestoreBackupRequestValidator(WebRestoreBackupRequestValidator webRestoreBackupRequestValidator) {
        this.webRestoreBackupRequestValidator = webRestoreBackupRequestValidator;
    }

    @Autowired
    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    @Autowired
    public void setBackupTaskManager(BackupTaskManager backupTaskManager) {
        this.backupTaskManager = backupTaskManager;
    }

    @Autowired
    public void setBackupPropertiesManager(BackupPropertiesManager backupPropertiesManager) {
        this.backupPropertiesManager = backupPropertiesManager;
    }

    @PostMapping(path = "/create-backup")
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

            Integer taskId = backupTaskManager.initNewTask(BackupTask.Type.CREATE_BACKUP, backupProperties);
            Future task = executorService.submit(
                    new Runnable() {
                        @Override
                        public void run() {
                            try {
                                backupTaskManager.updateTaskState(taskId, BackupTask.State.CREATING);

                                logger.info("createBackup(): Creating backup...");
                                InputStream backupStream = databaseBackupManager.createBackup(databaseSettings, taskId);

                                backupTaskManager.updateTaskState(taskId, BackupTask.State.APPLYING_PROCESSORS);

                                logger.info("createBackup(): Applying processors on created backup. Processors: {}", processorList);
                                InputStream processedBackupStream = backupProcessorManager.process(backupStream, processorList);

                                logger.info("createBackup(): Uploading backup...");

                                backupTaskManager.updateTaskState(taskId, BackupTask.State.UPLOADING);

                                backupLoadManager.uploadBackup(processedBackupStream, backupProperties, taskId);

                                backupTaskManager.updateTaskState(taskId, BackupTask.State.COMPLETED);

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

    @PostMapping(path = "/restore-backup")
    public String restoreBackup(WebRestoreBackupRequest webRestoreBackupRequest, BindingResult bindingResult) {
        logger.info("restoreBackup(): Got backup restoration job");

        webRestoreBackupRequestValidator.validate(webRestoreBackupRequest, bindingResult);
        if (bindingResult.hasErrors()) {
            return "dashboard";
        }

        Integer backupId = Integer.valueOf(webRestoreBackupRequest.getBackupId());
        BackupProperties backupProperties = backupPropertiesManager.findById(backupId).orElseThrow(() ->
                new RuntimeException(String.format(
                        "Can't retrieve backup properties. Error: no backup properties with ID %d", backupId)));

        String databaseSettingsName = webRestoreBackupRequest.getDatabaseSettingsName();
        DatabaseSettings databaseSettings = databaseSettingsManager.getById(databaseSettingsName).orElseThrow(() ->
                new RuntimeException(
                        String.format("Can't retrieve database settings. Error: no database settings with name %d", databaseSettingsName)));

        logger.info("restoreBackup(): Backup properties: {}", backupProperties);
        logger.info("restoreBackup(): Database settings: {}", databaseSettings);

        Integer taskId = backupTaskManager.initNewTask(BackupTask.Type.RESTORE_BACKUP, backupProperties);
        Future task = executorService.submit(
                new Runnable() {
                    @Override
                    public void run() {
                        try {
                            backupTaskManager.updateTaskState(taskId, BackupTask.State.DOWNLOADING);

                            logger.info("restoreBackup(): Downloading backup...");
                            InputStream downloadedBackup = backupLoadManager.downloadBackup(backupProperties, taskId);

                            backupTaskManager.updateTaskState(taskId, BackupTask.State.APPLYING_DEPROCESSORS);

                            logger.info("restoreBackup(): Deprocessing backup...");
                            InputStream deprocessedBackup = backupProcessorManager.deprocess(downloadedBackup, backupProperties.getProcessors());

                            backupTaskManager.updateTaskState(taskId, BackupTask.State.RESTORING);

                            logger.info("restoreBackup(): Restoring backup...");
                            databaseBackupManager.restoreBackup(deprocessedBackup, databaseSettings, taskId);

                            backupTaskManager.updateTaskState(taskId, BackupTask.State.COMPLETED);

                            logger.info("createBackup(): Restoring backup completed. Backup properties: {}", backupProperties);
                        } catch (RuntimeException ex) {
                            logger.info("restoreBackup(): Error occurred while restoring backup. Backup properties: {}",
                                    backupProperties, ex);
                            backupTaskManager.setError(taskId);
                        }
                    }
                }
        );
        backupTaskManager.addTaskFuture(taskId, task);

        return "redirect:/dashboard";
    }

    @Nullable
    private String validateDeleteBackupRequest(WebDeleteBackupRequest webDeleteBackupRequest) {
        String backupIdAsString = webDeleteBackupRequest.getBackupId();
        if (backupIdAsString.isEmpty() || backupIdAsString.trim().isEmpty()) {
            return "Please, provide backup ID to delete";
        }

        try {
            Integer.valueOf(backupIdAsString);
        } catch (NumberFormatException ex) {
            return "Invalid backup ID";
        }

        return null;
    }

    @DeleteMapping(path = "/delete-backup")
    public String deleteBackup(WebDeleteBackupRequest webDeleteBackupRequest) {
        logger.info("deleteBackup(): Got backup deletion job");

        String error = validateDeleteBackupRequest(webDeleteBackupRequest);
        if (error != null) {
            throw new ValidationError(error);
        }

        Integer backupId = Integer.valueOf(webDeleteBackupRequest.getBackupId());
        BackupProperties backupProperties = backupPropertiesManager.findById(backupId).orElseThrow(() ->
                new RuntimeException(String.format(
                        "Can't retrieve backup properties. Error: no backup properties with ID %d", backupId)));

        Integer taskId = backupTaskManager.initNewTask(BackupTask.Type.DELETE_BACKUP, backupProperties);

        backupPropertiesManager.deleteById(backupId);
        Future task = executorService.submit(
                new Runnable() {
                    @Override
                    public void run() {
                        try {
                            logger.info("deleteBackup(): Deleting backup started. Backup properties: {}", backupProperties);

                            backupTaskManager.updateTaskState(taskId, BackupTask.State.DELETING);

                            backupLoadManager.deleteBackup(backupProperties, taskId);

                            backupTaskManager.updateTaskState(taskId, BackupTask.State.COMPLETED);

                            logger.info("deleteBackup(): Deleting backup completed. Backup properties: {}", backupProperties);
                        } catch (RuntimeException ex) {
                            logger.info("deleteBackup(): Error occurred while deleting backup. Backup properties: {}",
                                    backupProperties, ex);
                            backupTaskManager.setError(taskId);
                        }
                    }

                });
        backupTaskManager.addTaskFuture(taskId, task);

        return "redirect:/dashboard";
    }
}
