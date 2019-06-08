package com.blog.controllers.WebApi;

import com.blog.controllers.Errors.ValidationException;
import com.blog.controllers.WebApi.Validator.WebAddPlannedTaskRequestValidator;
import com.blog.controllers.WebApi.Validator.WebCreateBackupRequestValidator;
import com.blog.controllers.WebApi.Validator.WebRestoreBackupRequestValidator;
import com.blog.entities.backup.BackupProperties;
import com.blog.entities.database.DatabaseSettings;
import com.blog.entities.storage.StorageSettings;
import com.blog.entities.task.PlannedTask;
import com.blog.entities.task.Task;
import com.blog.manager.*;
import com.blog.service.TasksStarterService;
import com.blog.webUI.formTransfer.WebAddPlannedTaskRequest;
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


/**
 * This controller is responsible for backup handling: creation, restoration and deletion of backup.
 */
@Controller
public class WebApiBackupController {
    private static final Logger logger = LoggerFactory.getLogger(WebApiBackupController.class);

    private DatabaseSettingsManager databaseSettingsManager;

    private StorageSettingsManager storageSettingsManager;

    private BackupProcessorManager backupProcessorManager;

    private WebCreateBackupRequestValidator webCreateBackupRequestValidator;

    private WebRestoreBackupRequestValidator webRestoreBackupRequestValidator;

    private WebAddPlannedTaskRequestValidator webAddPlannedTaskRequestValidator;

    private TasksManager tasksManager;

    private PlannedTasksManager plannedTasksManager;

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
    public void setWebAddPlannedTaskRequestValidator(WebAddPlannedTaskRequestValidator webAddPlannedTaskRequestValidator) {
        this.webAddPlannedTaskRequestValidator = webAddPlannedTaskRequestValidator;
    }

    @Autowired
    public void setTasksManager(TasksManager tasksManager) {
        this.tasksManager = tasksManager;
    }

    @Autowired
    public void setPlannedTasksManager(PlannedTasksManager plannedTasksManager) {
        this.plannedTasksManager = plannedTasksManager;
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
        DatabaseSettings databaseSettings = databaseSettingsManager.getById(databaseSettingsName).orElseThrow(() ->
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
        for (String processorName : webCreateBackupRequest.getProcessors()) {
            if (!backupProcessorManager.existsByName(processorName)) {
                throw new ValidationException("Can't create backup: invalid processor '" + processorName + "'");
            }
        }

        BackupProperties backupProperties = backupPropertiesManager.initNewBackupProperties(
                storageSettingsNameList, webCreateBackupRequest.getProcessors(), databaseSettings.getName());

        Integer taskId = tasksManager.initNewTask(Task.Type.CREATE_BACKUP, Task.RunType.USER, backupProperties.getId());
        tasksStarterService.startBackupTask(taskId, backupProperties, databaseSettings, logger);

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
        DatabaseSettings databaseSettings = databaseSettingsManager.getById(databaseSettingsName).orElseThrow(() ->
                new IllegalStateException("Can't restore backup: no such database settings with name " + databaseSettingsName));

        logger.info("restoreBackup(): Starting backup restoration... Backup properties: {}. Storage: {}. Database: {}",
                backupProperties, storageSettings, databaseSettings);

        Integer taskId = tasksManager.initNewTask(Task.Type.RESTORE_BACKUP, Task.RunType.USER, backupProperties.getId());
        tasksStarterService.startRestoreTask(taskId, backupProperties, storageSettingsName, databaseSettings, logger);

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
        tasksStarterService.startDeleteTask(taskId, backupProperties, logger);

        return "redirect:/dashboard";
    }

    @PostMapping(path = "/add-planned-task")
    public String addPlannedTask(WebAddPlannedTaskRequest webAddPlannedTaskRequest, BindingResult bindingResult) {
        logger.info("addPlannedTask(): Got planned task creation request");

        webAddPlannedTaskRequestValidator.validate(webAddPlannedTaskRequest, bindingResult);
        if (bindingResult.hasErrors()) {
            logger.error("Invalid planned task creation request. Error: {}", bindingResult.getAllErrors());

            return "dashboard";
        }

        String databaseSettingsName = webAddPlannedTaskRequest.getDatabaseSettingsName();
        if (!databaseSettingsManager.existsById(databaseSettingsName)) {
            throw new ValidationException(
                    String.format("Can't create planned task: Non-existing database: [%s]", databaseSettingsName));
        }

        List<String> storageSettingsNameList = webAddPlannedTaskRequest.getStorageSettingsNameList();
        for (String storageSettingsName : storageSettingsNameList) {
            if (!storageSettingsManager.existsById(storageSettingsName)) {
                throw new ValidationException(
                        String.format("Can't create planned task: Non-existing storage: [%s]", storageSettingsName));
            }
        }

        List<String> processors = webAddPlannedTaskRequest.getProcessors();
        for (String processorName : processors) {
            if (!backupProcessorManager.existsByName(processorName)) {
                throw new ValidationException(
                        String.format("Can't create planned task: Non-existing processor: [%s]", processorName));
            }
        }

        PlannedTask savedPlannedTask = plannedTasksManager.addNewTask(
                webAddPlannedTaskRequest.getDatabaseSettingsName(),
                webAddPlannedTaskRequest.getStorageSettingsNameList(), webAddPlannedTaskRequest.getProcessors(),
                Long.valueOf(webAddPlannedTaskRequest.getInterval()));

        logger.info("Planned backup task saved into database. Saved task: {}", savedPlannedTask);

        return "redirect:/dashboard";
    }
}
