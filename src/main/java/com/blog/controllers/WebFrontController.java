package com.blog.controllers;

import com.blog.entities.backup.BackupProperties;
import com.blog.entities.backup.BackupTask;
import com.blog.entities.database.DatabaseSettings;
import com.blog.entities.database.DatabaseType;
import com.blog.entities.storage.DropboxSettings;
import com.blog.entities.storage.LocalFileSystemSettings;
import com.blog.entities.storage.StorageSettings;
import com.blog.entities.storage.StorageType;
import com.blog.manager.BackupPropertiesManager;
import com.blog.manager.BackupTaskManager;
import com.blog.manager.DatabaseSettingsManager;
import com.blog.manager.StorageSettingsManager;
import com.blog.webUI.formTransfer.*;
import com.blog.webUI.renderModels.WebBackupItem;
import com.blog.webUI.renderModels.WebBackupTask;
import com.blog.webUI.renderModels.WebDatabaseItem;
import com.blog.webUI.renderModels.WebStorageItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Controller
@ControllerAdvice
public class WebFrontController {
    private SimpleDateFormat dateFormat;

    private DatabaseSettingsManager databaseSettingsManager;

    private StorageSettingsManager storageSettingsManager;

    private BackupPropertiesManager backupPropertiesManager;

    private BackupTaskManager backupTaskManager;

    @Autowired
    public void setDateFormat(SimpleDateFormat dateFormat) {
        this.dateFormat = dateFormat;
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
    public void setBackupPropertiesManager(BackupPropertiesManager backupPropertiesManager) {
        this.backupPropertiesManager = backupPropertiesManager;
    }

    @Autowired
    public void setBackupTaskManager(BackupTaskManager backupTaskManager) {
        this.backupTaskManager = backupTaskManager;
    }

    @RequestMapping("/")
    public String index() {
        return "redirect:/dashboard";
    }

    @RequestMapping("/login")
    public String login() {
        return "login";
    }

    @ModelAttribute
    public void addLists(Model model) {
        // add storage settings
        {
            List<WebStorageItem> storageList = new ArrayList<>();

            // add local file system settings
            for (StorageSettings storageSettings : storageSettingsManager.getAllByType(StorageType.LOCAL_FILE_SYSTEM)) {
                LocalFileSystemSettings localFileSystemSettings = storageSettings.getLocalFileSystemSettings().orElseThrow(
                        RuntimeException::new);

                HashMap<String, String> storageProperties = new HashMap<>();
                storageProperties.put("Settings name", storageSettings.getSettingsName());
                storageProperties.put("Backup path", localFileSystemSettings.getBackupPath());

                WebStorageItem storageItem = new WebStorageItem.Builder()
                        .withType(storageSettings.getType())
                        .withSettingsName(storageSettings.getSettingsName())
                        .withDesc(storageProperties.toString())
                        .withTime(dateFormat.format(storageSettings.getDate()))
                        .build();

                storageList.add(storageItem);
            }

            // add dropbox settings
            for (StorageSettings storageSettings : storageSettingsManager.getAllByType(StorageType.DROPBOX)) {
                DropboxSettings dropboxSettings = storageSettings.getDropboxSettings().orElseThrow(RuntimeException::new);

                HashMap<String, String> storageProperties = new HashMap<>();
                storageProperties.put("Settings name", storageSettings.getSettingsName());
                storageProperties.put("Access token", dropboxSettings.getAccessToken());

                WebStorageItem storageItem = new WebStorageItem.Builder()
                        .withType(storageSettings.getType())
                        .withSettingsName(storageSettings.getSettingsName())
                        .withDesc(storageProperties.toString())
                        .withTime(dateFormat.format(storageSettings.getDate()))
                        .build();

                storageList.add(storageItem);
            }

            model.addAttribute("storageList", storageList);
        }

        // add database settings
        {
            List<WebDatabaseItem> databaseList = new ArrayList<>();

            for (DatabaseSettings databaseSettings : databaseSettingsManager.getAllByType(DatabaseType.POSTGRES)) {
                HashMap<String, String> databaseProperties = new HashMap<>();
                databaseProperties.put("Settings name", databaseSettings.getSettingsName());
                databaseProperties.put("Host", databaseSettings.getHost());
                databaseProperties.put("Port", Integer.toString(databaseSettings.getPort()));
                databaseProperties.put("Database name", databaseSettings.getName());

                WebDatabaseItem databaseItem = new WebDatabaseItem.Builder()
                        .withType(databaseSettings.getType())
                        .withSettingsName(databaseSettings.getSettingsName())
                        .withDesc(databaseProperties.toString())
                        .withTime(dateFormat.format(databaseSettings.getDate()))
                        .build();

                databaseList.add(databaseItem);
            }

            model.addAttribute("databaseList", databaseList);
        }

        // add created backup list
        {
            List<WebBackupItem> backupList = new ArrayList<>();

            for (BackupProperties currentBackupProperties : backupPropertiesManager.findAll()) {
                HashMap<String, String> backupProperties = new HashMap<>();

                String storageSettingsName = currentBackupProperties.getStorageSettingsName();
                StorageSettings storageSettings = storageSettingsManager.getById(storageSettingsName).orElseThrow(() ->
                        new RuntimeException(String.format("Error occurred while rendering page: Missing " +
                                "storage settings with name %d", storageSettingsName)));

                backupProperties.put("Processors", currentBackupProperties.getProcessors().toString());
                backupProperties.put("Stored on", storageSettings.getType().toString());

                WebBackupItem webBackupItem = new WebBackupItem.Builder()
                        .withId(currentBackupProperties.getId())
                        .withDesc(backupProperties.toString())
                        .withName(currentBackupProperties.getBackupName())
                        .withTime(dateFormat.format(currentBackupProperties.getDate()))
                        .build();

                backupList.add(webBackupItem);
            }

            model.addAttribute("backupList", backupList);
        }

        // add executing backup task list
        {
            List<WebBackupTask> backupTaskList = new ArrayList<>();

            for (BackupTask backupTask : backupTaskManager.findAllByRunType(BackupTask.RunType.USER)) {
                WebBackupTask webBackupTask = new WebBackupTask.Builder()
                        .withId(backupTask.getId())
                        .withType(backupTask.getType().toString())
                        .withState(backupTask.getState().toString())
                        .withIsError(backupTask.isError())
                        .withTime(dateFormat.format(backupTask.getDate()))
                        .build();

                backupTaskList.add(webBackupTask);
            }

            model.addAttribute("backupTasks", backupTaskList);
        }

        // add forms for input
        model.addAttribute("webAddDatabaseRequest", new WebAddDatabaseRequest());
        model.addAttribute("webAddStorageRequest", new WebAddStorageRequest());
        model.addAttribute("webCreateBackupRequest", new WebCreateBackupRequest());
        model.addAttribute("webRestoreBackupRequest", new WebRestoreBackupRequest());
        model.addAttribute("webAddPlannedTaskRequest", new WebAddPlannedTaskRequest());
    }

    @RequestMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }
}
