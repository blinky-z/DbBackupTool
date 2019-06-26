package com.blog.controllers.WebApi;

import com.blog.controllers.Errors.ValidationException;
import com.blog.controllers.WebApi.Validator.WebAddPlannedTaskRequestValidator;
import com.blog.entities.task.PlannedTask;
import com.blog.manager.CancelTasksManager;
import com.blog.manager.DatabaseSettingsManager;
import com.blog.manager.PlannedTasksManager;
import com.blog.manager.StorageSettingsManager;
import com.blog.service.processor.ProcessorType;
import com.blog.webUI.formTransfer.WebAddPlannedTaskRequest;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * This controller is responsible for tasks handling: canceling, adding planned tasks.
 */
@Controller
public class WebApiTaskController {
    private static final Logger logger = LoggerFactory.getLogger(WebApiTaskController.class);

    private WebAddPlannedTaskRequestValidator webAddPlannedTaskRequestValidator;

    private DatabaseSettingsManager databaseSettingsManager;

    private StorageSettingsManager storageSettingsManager;

    private PlannedTasksManager plannedTasksManager;

    private CancelTasksManager cancelTasksManager;

    @Autowired
    public void setWebAddPlannedTaskRequestValidator(WebAddPlannedTaskRequestValidator webAddPlannedTaskRequestValidator) {
        this.webAddPlannedTaskRequestValidator = webAddPlannedTaskRequestValidator;
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
    public void setPlannedTasksManager(PlannedTasksManager plannedTasksManager) {
        this.plannedTasksManager = plannedTasksManager;
    }

    @Autowired
    public void setCancelTasksManager(CancelTasksManager cancelTasksManager) {
        this.cancelTasksManager = cancelTasksManager;
    }

    @PostMapping(path = "/planned-task")
    public String addPlannedTask(WebAddPlannedTaskRequest webAddPlannedTaskRequest, BindingResult bindingResult) {
        logger.info("addPlannedTask(): Got planned task creation request");

        webAddPlannedTaskRequestValidator.validate(webAddPlannedTaskRequest, bindingResult);
        if (bindingResult.hasErrors()) {
            logger.error("addPlannedTask(): Invalid planned task creation request. Error: {}", bindingResult.getAllErrors());

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
                throw new ValidationException(String.format("Can't create planned task: Non-existing storage: [%s]", storageSettingsName));
            }
        }

        List<ProcessorType> processors = new ArrayList<>();
        for (String processorName : webAddPlannedTaskRequest.getProcessors()) {
            Optional<ProcessorType> optionalProcessorType = ProcessorType.of(processorName);
            if (!optionalProcessorType.isPresent()) {
                throw new ValidationException(String.format("Can't create planned task: Non-existing processor: [%s]", processorName));
            }
            processors.add(optionalProcessorType.get());
        }

        PlannedTask savedPlannedTask = plannedTasksManager.addNewTask(
                webAddPlannedTaskRequest.getDatabaseSettingsName(),
                webAddPlannedTaskRequest.getStorageSettingsNameList(), processors,
                Long.valueOf(webAddPlannedTaskRequest.getInterval()));

        logger.info("addPlannedTask(): Planned backup task saved into database. Saved task: {}", savedPlannedTask);

        return "redirect:/dashboard";
    }

    @Nullable
    private String validateCancelTaskRequest(@Nullable String taskId) {
        if (taskId == null || taskId.trim().isEmpty()) {
            return "Please, provide task ID to cancel";
        }

        try {
            Integer.valueOf(taskId);
        } catch (NumberFormatException ex) {
            return "Invalid ID: " + taskId;
        }

        return null;
    }

    @PostMapping(path = "/cancel-task")
    public String cancelTask(@RequestParam(value = "taskId") Optional<String> taskId) {
        logger.info("cancelTask(): Got task cancellation request");

        String error = validateCancelTaskRequest(taskId.orElse(null));
        if (error != null) {
            throw new ValidationException(error);
        }

        Integer id = Integer.valueOf(taskId.get());
        cancelTasksManager.addTaskToCancel(id);

        return "redirect:/dashboard";
    }
}
