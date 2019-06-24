package com.blog.controllers;

import com.blog.entities.backup.BackupProperties;
import com.blog.entities.database.DatabaseSettings;
import com.blog.entities.database.DatabaseType;
import com.blog.entities.storage.DropboxSettings;
import com.blog.entities.storage.LocalFileSystemSettings;
import com.blog.entities.storage.StorageSettings;
import com.blog.entities.storage.StorageType;
import com.blog.entities.task.ErrorTask;
import com.blog.entities.task.Task;
import com.blog.manager.*;
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

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Controller
@ControllerAdvice
public class WebFrontController {
    private DateTimeFormatter webDateFormatter;

    private DatabaseSettingsManager databaseSettingsManager;

    private StorageSettingsManager storageSettingsManager;

    private BackupPropertiesManager backupPropertiesManager;

    private TasksManager tasksManager;

    private ErrorTasksManager errorTasksManager;

    @Autowired
    public void setWebDateFormatter(DateTimeFormatter webDateFormatter) {
        this.webDateFormatter = webDateFormatter;
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
    public void setTasksManager(TasksManager tasksManager) {
        this.tasksManager = tasksManager;
    }

    @Autowired
    public void setErrorTasksManager(ErrorTasksManager errorTasksManager) {
        this.errorTasksManager = errorTasksManager;
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
            for (StorageSettings storageSettings : storageSettingsManager.findAllByType(StorageType.LOCAL_FILE_SYSTEM)) {
                Optional<LocalFileSystemSettings> optionalLocalFileSystemSettings = storageSettings.getLocalFileSystemSettings();
                if (!optionalLocalFileSystemSettings.isPresent()) {
                    continue;
                }
                LocalFileSystemSettings localFileSystemSettings = optionalLocalFileSystemSettings.get();

                HashMap<String, String> webStorageProperties = new HashMap<>();
                webStorageProperties.put("Settings name", storageSettings.getSettingsName());
                webStorageProperties.put("Backup path", localFileSystemSettings.getBackupPath());

                WebStorageItem storageItem = new WebStorageItem.Builder()
                        .withType(storageSettings.getType())
                        .withSettingsName(storageSettings.getSettingsName())
                        .withDesc(webStorageProperties.toString())
                        .withTime(webDateFormatter.format(storageSettings.getDate()))
                        .build();

                storageList.add(storageItem);
            }

            // add dropbox settings
            for (StorageSettings storageSettings : storageSettingsManager.findAllByType(StorageType.DROPBOX)) {
                Optional<DropboxSettings> optionalDropboxSettings = storageSettings.getDropboxSettings();
                if (!optionalDropboxSettings.isPresent()) {
                    continue;
                }
                DropboxSettings dropboxSettings = optionalDropboxSettings.get();

                HashMap<String, String> webStorageProperties = new HashMap<>();
                webStorageProperties.put("Settings name", storageSettings.getSettingsName());
                webStorageProperties.put("Access token", dropboxSettings.getAccessToken());

                WebStorageItem storageItem = new WebStorageItem.Builder()
                        .withType(storageSettings.getType())
                        .withSettingsName(storageSettings.getSettingsName())
                        .withTime(webDateFormatter.format(storageSettings.getDate()))
                        .withDesc(webStorageProperties.toString())
                        .build();

                storageList.add(storageItem);
            }

            model.addAttribute("storageList", storageList);
        }

        // add database settings
        {
            List<WebDatabaseItem> databaseList = new ArrayList<>();

            for (DatabaseSettings databaseSettings : databaseSettingsManager.findAllByType(DatabaseType.POSTGRES)) {
                HashMap<String, String> webDatabaseProperties = new HashMap<>();
                webDatabaseProperties.put("Settings name", databaseSettings.getSettingsName());
                webDatabaseProperties.put("Host", databaseSettings.getHost());
                webDatabaseProperties.put("Port", Integer.toString(databaseSettings.getPort()));
                webDatabaseProperties.put("Database name", databaseSettings.getName());

                WebDatabaseItem databaseItem = new WebDatabaseItem.Builder()
                        .withType(databaseSettings.getType())
                        .withSettingsName(databaseSettings.getSettingsName())
                        .withTime(webDateFormatter.format(databaseSettings.getDate()))
                        .withDesc(webDatabaseProperties.toString())
                        .build();

                databaseList.add(databaseItem);
            }

            model.addAttribute("databaseList", databaseList);
        }

        // add created backup list
        {
            List<WebBackupItem> backupList = new ArrayList<>();

            for (BackupProperties backupProperties : backupPropertiesManager.findAll()) {
                HashMap<String, String> webBackupProperties = new HashMap<>();

                webBackupProperties.put("Processors", backupProperties.getProcessors().toString());

                WebBackupItem webBackupItem = new WebBackupItem.Builder()
                        .withId(backupProperties.getId())
                        .withStorageNames(backupProperties.getStorageSettingsNameList())
                        .withName(backupProperties.getBackupName())
                        .withTime(webDateFormatter.format(backupProperties.getDate()))
                        .withDesc(webBackupProperties.toString())
                        .build();

                backupList.add(webBackupItem);
            }

            model.addAttribute("backupList", backupList);
        }

        // add executing backup task list
        {
            List<WebBackupTask> backupTaskList = new ArrayList<>();

            Iterable<Task> tasks = tasksManager.findAllByRunType(Task.RunType.USER);
            List<Integer> taskIds = StreamSupport.stream(tasks.spliterator(), false).map(Task::getId).collect(Collectors.toList());
            HashSet<Integer> errorTaskIds = StreamSupport.stream(errorTasksManager.findAllByTaskIdIn(taskIds).spliterator(), false)
                    .map(ErrorTask::getTaskId)
                    .collect(Collectors.toCollection(HashSet::new));

            for (Task task : tasksManager.findAllByRunType(Task.RunType.USER)) {
                WebBackupTask webBackupTask = new WebBackupTask.Builder()
                        .withId(task.getId())
                        .withType(task.getType().toString())
                        .withState(task.getState().toString())
                        .withIsError(errorTaskIds.contains(task.getId()))
                        .withIsInterrupted(task.getInterrupted())
                        .withTime(webDateFormatter.format(task.getDate()))
                        .build();

                backupTaskList.add(webBackupTask);
            }

            model.addAttribute("backupTasks", backupTaskList);
        }

        // add input forms
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
