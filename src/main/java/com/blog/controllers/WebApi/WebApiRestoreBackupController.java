package com.blog.controllers.WebApi;

import com.blog.controllers.WebApi.Validator.WebRestoreBackupRequestValidator;
import com.blog.entities.backup.BackupProperties;
import com.blog.entities.backup.BackupTaskState;
import com.blog.entities.database.DatabaseSettings;
import com.blog.manager.*;
import com.blog.webUI.formTransfer.WebRestoreBackupRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.InputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

@Controller
@RequestMapping("/restore-backup")
public class WebApiRestoreBackupController {
    private static final Logger logger = LoggerFactory.getLogger(WebApiRestoreBackupController.class);

    private DatabaseBackupManager databaseBackupManager;

    private BackupPropertiesManager backupPropertiesManager;

    private DatabaseSettingsManager databaseSettingsManager;

    private BackupLoadManager backupLoadManager;

    private BackupProcessorManager backupProcessorManager;

    private WebRestoreBackupRequestValidator webRestoreBackupRequestValidator;

    private ExecutorService executorService;

    private BackupTaskManager backupTaskManager;

    @Autowired
    public void setBackupTaskManager(BackupTaskManager backupTaskManager) {
        this.backupTaskManager = backupTaskManager;
    }

    @Autowired
    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    @Autowired
    public void setDatabaseBackupManager(DatabaseBackupManager databaseBackupManager) {
        this.databaseBackupManager = databaseBackupManager;
    }

    @Autowired
    public void setBackupPropertiesManager(BackupPropertiesManager backupPropertiesManager) {
        this.backupPropertiesManager = backupPropertiesManager;
    }

    @Autowired
    public void setDatabaseSettingsManager(DatabaseSettingsManager databaseSettingsManager) {
        this.databaseSettingsManager = databaseSettingsManager;
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
    public void setWebRestoreBackupRequestValidator(WebRestoreBackupRequestValidator webRestoreBackupRequestValidator) {
        this.webRestoreBackupRequestValidator = webRestoreBackupRequestValidator;
    }

    @PostMapping
    public String restoreBackup(WebRestoreBackupRequest webRestoreBackupRequest, BindingResult bindingResult) {
        logger.info("restoreBackup(): Got backup restoration job");

        webRestoreBackupRequestValidator.validate(webRestoreBackupRequest, bindingResult);
        if (bindingResult.hasErrors()) {
            logger.info("Has errors: {}", bindingResult.getAllErrors());

            return "dashboard";
        }

        Integer backupId = Integer.valueOf(webRestoreBackupRequest.getBackupId());
        BackupProperties backupProperties = backupPropertiesManager.getById(backupId).orElseThrow(() ->
                new RuntimeException(String.format(
                        "Can't retrieve backup properties. Error: no backup properties with ID %d", backupId)));

        String databaseSettingsName = webRestoreBackupRequest.getDatabaseSettingsName();
        DatabaseSettings databaseSettings = databaseSettingsManager.getById(databaseSettingsName).orElseThrow(() ->
                new RuntimeException(
                        String.format("Can't retrieve database settings. Error: no database settings with name %d", databaseSettingsName)));

        logger.info("restoreBackup(): Backup properties: {}", backupProperties);
        logger.info("restoreBackup(): Database settings: {}", databaseSettings);

        Integer taskId = backupTaskManager.initNewTask();
        Future<BackupProperties> task = executorService.submit(
                new Callable<BackupProperties>() {
                    @Override
                    public BackupProperties call() {
                        try {
                            backupTaskManager.updateTaskState(taskId, BackupTaskState.DOWNLOADING);

                            logger.info("restoreBackup(): Downloading backup...");
                            InputStream downloadedBackup = backupLoadManager.downloadBackup(backupProperties);

                            backupTaskManager.updateTaskState(taskId, BackupTaskState.APPLYING_DEPROCESSORS);

                            logger.info("restoreBackup(): Deprocessing backup...");
                            InputStream deprocessedBackup = backupProcessorManager.deprocess(downloadedBackup, backupProperties.getProcessors());

                            backupTaskManager.updateTaskState(taskId, BackupTaskState.RESTORING);

                            logger.info("restoreBackup(): Restoring backup...");
                            databaseBackupManager.restoreBackup(deprocessedBackup, databaseSettings, taskId);

                            backupTaskManager.updateTaskState(taskId, BackupTaskState.COMPLETED);

                            logger.info("createBackup(): Restoring backup completed. Backup properties: {}", backupProperties);
                        } catch (RuntimeException ex) {
                            logger.info("restoreBackup(): Error occurred while restoring backup. Backup properties: {}",
                                    backupProperties, ex);
                            backupTaskManager.setError(taskId);
                        }

                        return backupProperties;
                    }
                }
        );
        backupTaskManager.addTaskFuture(taskId, task);

        return "redirect:/dashboard";
    }
}
