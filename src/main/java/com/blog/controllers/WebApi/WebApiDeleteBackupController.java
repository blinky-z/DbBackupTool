package com.blog.controllers.WebApi;

import com.blog.controllers.Errors.ValidationError;
import com.blog.controllers.WebApi.Validator.WebDeleteBackupRequestValidator;
import com.blog.entities.backup.BackupProperties;
import com.blog.entities.backup.BackupTaskState;
import com.blog.entities.backup.BackupTaskType;
import com.blog.manager.BackupLoadManager;
import com.blog.manager.BackupPropertiesManager;
import com.blog.manager.BackupTaskManager;
import com.blog.webUI.formTransfer.WebDeleteBackupRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

@Controller
@RequestMapping("/delete-backup")
public class WebApiDeleteBackupController {
    private static final Logger logger = LoggerFactory.getLogger(WebApiDeleteBackupController.class);

    private BackupPropertiesManager backupPropertiesManager;

    private BackupLoadManager backupLoadManager;

    private WebDeleteBackupRequestValidator webDeleteBackupRequestValidator;

    private BackupTaskManager backupTaskManager;

    private ExecutorService executorService;

    @Autowired
    public void setBackupLoadManager(BackupLoadManager backupLoadManager) {
        this.backupLoadManager = backupLoadManager;
    }

    @Autowired
    public void setBackupTaskManager(BackupTaskManager backupTaskManager) {
        this.backupTaskManager = backupTaskManager;
    }

    @Autowired
    public void setWebDeleteBackupRequestValidator(WebDeleteBackupRequestValidator webDeleteBackupRequestValidator) {
        this.webDeleteBackupRequestValidator = webDeleteBackupRequestValidator;
    }

    @Autowired
    public void setBackupPropertiesManager(BackupPropertiesManager backupPropertiesManager) {
        this.backupPropertiesManager = backupPropertiesManager;
    }

    @Autowired
    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    @DeleteMapping
    public String deleteBackup(WebDeleteBackupRequest webDeleteBackupRequest, BindingResult bindingResult) {
        logger.info("deleteBackup(): Got backup deletion job");

        webDeleteBackupRequestValidator.validate(webDeleteBackupRequest, bindingResult);
        if (bindingResult.hasErrors()) {
            throw new ValidationError("Invalid backup deletion request");
        }

        Integer backupId = Integer.valueOf(webDeleteBackupRequest.getBackupId());
        BackupProperties backupProperties = backupPropertiesManager.findById(backupId).orElseThrow(() ->
                new RuntimeException(String.format(
                        "Can't retrieve backup properties. Error: no backup properties with ID %d", backupId)));

        Integer taskId = backupTaskManager.initNewTask(BackupTaskType.DELETE_BACKUP, backupProperties);

        backupPropertiesManager.deleteById(backupId);
        Future task = executorService.submit(
                new Runnable() {
                    @Override
                    public void run() {
                        try {
                            logger.info("deleteBackup(): Deleting backup started. Backup properties: {}", backupProperties);

                            backupTaskManager.updateTaskState(taskId, BackupTaskState.DELETING);

                            backupLoadManager.deleteBackup(backupProperties);

                            backupTaskManager.updateTaskState(taskId, BackupTaskState.COMPLETED);

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
