package com.blog.controllers.WebApi;

import com.blog.controllers.Errors.ValidationError;
import com.blog.controllers.WebApi.Validator.WebDeleteBackupRequestValidator;
import com.blog.entities.backup.BackupProperties;
import com.blog.manager.BackupLoadManager;
import com.blog.manager.BackupPropertiesManager;
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

    private ExecutorService executorService;

    @Autowired
    public void setBackupLoadManager(BackupLoadManager backupLoadManager) {
        this.backupLoadManager = backupLoadManager;
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
        logger.info("restoreBackup(): Got backup deletion job");

        webDeleteBackupRequestValidator.validate(webDeleteBackupRequest, bindingResult);
        if (bindingResult.hasErrors()) {
            logger.info("Has errors: {}", bindingResult.getAllErrors());

            throw new ValidationError("Invalid backup deletion request");
        }

        Integer backupId = Integer.valueOf(webDeleteBackupRequest.getBackupId());
        BackupProperties backupProperties = backupPropertiesManager.getById(backupId).orElseThrow(() ->
                new RuntimeException(String.format(
                        "Can't retrieve backup properties. Error: no backup properties with ID %d", backupId)));

        backupPropertiesManager.deleteById(backupId);
        Future task = executorService.submit(
                new Runnable() {
                    @Override
                    public void run() {
                        backupLoadManager.deleteBackup(backupProperties);
                    }
                });

        return "redirect:/dashboard";
    }
}
