package com.blog.controllers.WebApi;

import com.blog.controllers.Errors.ValidationException;
import com.blog.controllers.WebApi.Validator.WebCreateBackupRequestValidator;
import com.blog.controllers.WebApi.Validator.WebRestoreBackupRequestValidator;
import com.blog.entities.backup.BackupProperties;
import com.blog.entities.database.DatabaseSettings;
import com.blog.entities.storage.StorageSettings;
import com.blog.entities.task.Task;
import com.blog.manager.BackupPropertiesManager;
import com.blog.manager.DatabaseSettingsManager;
import com.blog.manager.StorageSettingsManager;
import com.blog.manager.TasksManager;
import com.blog.service.TasksStarterService;
import com.blog.service.processor.ProcessorType;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


/**
 * This controller is responsible for backup handling: creation, restoration and deletion of backup.
 */
@Controller
public class WebApiBackupController {
    private static final Logger logger = LoggerFactory.getLogger(WebApiBackupController.class);

    private DatabaseSettingsManager databaseSettingsManager;

    private StorageSettingsManager storageSettingsManager;

    private WebCreateBackupRequestValidator webCreateBackupRequestValidator;

    private WebRestoreBackupRequestValidator webRestoreBackupRequestValidator;

    private TasksManager tasksManager;

    private BackupPropertiesManager backupPropertiesManager;

    private TasksStarterService tasksStarterService;

    @Autowired
    public void setDatabaseSettingsManager(DatabaseSettingsManager databaseSettingsManager) {
        this.databaseSettingsManager = databaseSettingsManager;
    }

    @Autowired
    public void setStorageSettingsManager(StorageSettingsManager storageSettingsManager) {
        this.storageSettingsManager = storageSettingsManager;
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
    public void setTasksManager(TasksManager tasksManager) {
        this.tasksManager = tasksManager;
    }

    @Autowired
    public void setBackupPropertiesManager(BackupPropertiesManager backupPropertiesManager) {
        this.backupPropertiesManager = backupPropertiesManager;
    }

    @Autowired
    public void setTasksStarterService(TasksStarterService tasksStarterService) {
        this.tasksStarterService = tasksStarterService;
    }

    @PostMapping(path = "/create-backup")
    public String createBackup(WebCreateBackupRequest webCreateBackupRequest,
                               BindingResult bindingResult) {
        logger.info("createBackup(): Got backup creation request");

        webCreateBackupRequestValidator.validate(webCreateBackupRequest, bindingResult);
        if (bindingResult.hasErrors()) {
            logger.error("Invalid backup creation request. Errors: {}", bindingResult.getAllErrors());

            return "dashboard";
        }

        String databaseSettingsName = webCreateBackupRequest.getDatabaseSettingsName();
        DatabaseSettings databaseSettings = databaseSettingsManager.findById(databaseSettingsName).orElseThrow(() ->
                new IllegalStateException("Can't retrieve database settings. Error: no database settings with name " + databaseSettingsName));

        logger.info("createBackup(): Database settings: {}", databaseSettings);

        List<String> storageSettingsNameList = new ArrayList<>();
        for (String storageSettingsName : webCreateBackupRequest.getStorageSettingsNameList()) {
            if (!storageSettingsManager.existsById(storageSettingsName)) {
                logger.error("createBackup(): No storage settings with name {}. Skipping this storage", storageSettingsName);
                continue;
            }
            storageSettingsNameList.add(storageSettingsName);
        }
        List<ProcessorType> processors = new ArrayList<>();
        for (String processorName : webCreateBackupRequest.getProcessors()) {
            Optional<ProcessorType> optionalProcessorType = ProcessorType.of(processorName);
            if (!optionalProcessorType.isPresent()) {
                throw new ValidationException("Can't create backup: invalid processor '" + processorName + "'");
            }
            processors.add(optionalProcessorType.get());
        }

        BackupProperties backupProperties = backupPropertiesManager.initNewBackupProperties(
                storageSettingsNameList, processors, databaseSettings.getName());

        Integer taskId = tasksManager.initNewTask(Task.Type.CREATE_BACKUP, Task.RunType.USER, backupProperties.getId());
        tasksStarterService.startBackupTask(taskId, backupProperties, databaseSettings);

        return "redirect:/dashboard";
    }

    @PostMapping(path = "/restore-backup")
    public String restoreBackup(WebRestoreBackupRequest webRestoreBackupRequest, BindingResult bindingResult) {
        logger.info("restoreBackup(): Got backup restoration request");

        webRestoreBackupRequestValidator.validate(webRestoreBackupRequest, bindingResult);
        if (bindingResult.hasErrors()) {
            logger.error("Invalid backup restoration request. Errors: {}", bindingResult.getAllErrors());

            return "dashboard";
        }

        Integer backupId = Integer.valueOf(webRestoreBackupRequest.getBackupId());
        BackupProperties backupProperties = backupPropertiesManager.findById(backupId).orElseThrow(() ->
                new IllegalStateException("Can't restore backup: no such backup properties with ID " + backupId));

        String storageSettingsName = webRestoreBackupRequest.getStorageSettingsName();
        StorageSettings storageSettings = storageSettingsManager.findById(storageSettingsName).orElseThrow(() ->
                new IllegalStateException("Can't restore backup: no such storage settings with name " + storageSettingsName));

        String databaseSettingsName = webRestoreBackupRequest.getDatabaseSettingsName();
        DatabaseSettings databaseSettings = databaseSettingsManager.findById(databaseSettingsName).orElseThrow(() ->
                new IllegalStateException("Can't restore backup: no such database settings with name " + databaseSettingsName));

        logger.info("restoreBackup(): Starting backup restoration... Backup properties: {}. Storage: {}. Database: {}",
                backupProperties, storageSettings, databaseSettings);

        Integer taskId = tasksManager.initNewTask(Task.Type.RESTORE_BACKUP, Task.RunType.USER, backupProperties.getId());
        tasksStarterService.startRestoreTask(taskId, backupProperties, storageSettingsName, databaseSettings);

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
        logger.info("deleteBackup(): Got backup deletion request");

        String error = validateDeleteBackupRequest(webDeleteBackupRequest);
        if (error != null) {
            logger.error("Invalid backup deletion request. Error: {}", error);

            throw new ValidationException(error);
        }

        Integer backupId = Integer.valueOf(webDeleteBackupRequest.getBackupId());
        BackupProperties backupProperties = backupPropertiesManager.findById(backupId).orElseThrow(() ->
                new IllegalStateException("Can't delete backup: no such backup properties with ID " + backupId));

        Integer taskId = tasksManager.initNewTask(Task.Type.DELETE_BACKUP, Task.RunType.USER, backupProperties.getId());

        backupPropertiesManager.deleteById(backupId);
        tasksStarterService.startDeleteTask(taskId, backupProperties);

        return "redirect:/dashboard";
    }
}
