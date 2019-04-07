package com.blog.controllers;

import com.blog.entities.backup.BackupProperties;
import com.blog.entities.database.DatabaseSettings;
import com.blog.entities.database.DatabaseType;
import com.blog.entities.database.PostgresSettings;
import com.blog.entities.storage.DropboxSettings;
import com.blog.entities.storage.LocalFileSystemSettings;
import com.blog.entities.storage.StorageSettings;
import com.blog.entities.storage.StorageType;
import com.blog.manager.BackupPropertiesManager;
import com.blog.manager.DatabaseSettingsManager;
import com.blog.manager.StorageSettingsManager;
import com.blog.webUI.formTransfer.WebAddDatabaseRequest;
import com.blog.webUI.formTransfer.WebAddStorageRequest;
import com.blog.webUI.formTransfer.WebCreateBackupRequest;
import com.blog.webUI.formTransfer.WebRestoreBackupRequest;
import com.blog.webUI.renderModels.WebBackupItem;
import com.blog.webUI.renderModels.WebDatabaseItem;
import com.blog.webUI.renderModels.WebStorageItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger logger = LoggerFactory.getLogger(WebFrontController.class);

    private static final String TIME_FORMAT = "dd.MM.yyyy HH:mm:ss";

    private DatabaseSettingsManager databaseSettingsManager;

    private StorageSettingsManager storageSettingsManager;

    private BackupPropertiesManager backupPropertiesManager;

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
        logger.info("Adding lists...");

        SimpleDateFormat dateFormat = new SimpleDateFormat(TIME_FORMAT);

        {
            List<WebStorageItem> storageList = new ArrayList<>();

            for (StorageSettings storageSettings : storageSettingsManager.getAllByType(StorageType.LOCAL_FILE_SYSTEM)) {
                LocalFileSystemSettings localFileSystemSettings = storageSettings.getLocalFileSystemSettings().orElseThrow(
                        RuntimeException::new);

                HashMap<String, String> storageProperties = new HashMap<>();
                storageProperties.put("Settings name", storageSettings.getSettingsName());
                storageProperties.put("Backup path", localFileSystemSettings.getBackupPath());

                WebStorageItem storageItem = new WebStorageItem(storageSettings.getType(), storageSettings.getSettingsName(),
                        storageProperties.toString(), dateFormat.format(storageSettings.getDate()));
                storageList.add(storageItem);
            }
            for (StorageSettings storageSettings : storageSettingsManager.getAllByType(StorageType.DROPBOX)) {
                DropboxSettings dropboxSettings = storageSettings.getDropboxSettings().orElseThrow(RuntimeException::new);

                HashMap<String, String> storageProperties = new HashMap<>();
                storageProperties.put("Settings name", storageSettings.getSettingsName());
                storageProperties.put("Access token", dropboxSettings.getAccessToken());

                WebStorageItem storageItem = new WebStorageItem(storageSettings.getType(), storageSettings.getSettingsName(),
                        storageProperties.toString(), dateFormat.format(storageSettings.getDate()));
                storageList.add(storageItem);
            }

            model.addAttribute("storageList", storageList);
        }

        {
            List<WebDatabaseItem> databaseList = new ArrayList<>();

            for (DatabaseSettings databaseSettings : databaseSettingsManager.getAllByType(DatabaseType.POSTGRES)) {
                PostgresSettings postgresSettings = databaseSettings.getPostgresSettings().orElseThrow(RuntimeException::new);

                HashMap<String, String> databaseProperties = new HashMap<>();
                databaseProperties.put("Settings name", databaseSettings.getSettingsName());
                databaseProperties.put("Host", databaseSettings.getHost());
                databaseProperties.put("Port", Integer.toString(databaseSettings.getPort()));
                databaseProperties.put("Database name", databaseSettings.getName());

                WebDatabaseItem databaseItem = new WebDatabaseItem(databaseSettings.getType(), databaseSettings.getSettingsName(),
                        databaseProperties.toString(), dateFormat.format(databaseSettings.getDate()));
                databaseList.add(databaseItem);
            }

            model.addAttribute("databaseList", databaseList);
        }

        {
            List<WebBackupItem> backupList = new ArrayList<>();

            for (BackupProperties currentBackupProperties : backupPropertiesManager.getAll()) {
                HashMap<String, String> backupProperties = new HashMap<>();

                String storageSettingsName = currentBackupProperties.getStorageSettingsName();
                StorageSettings storageSettings = storageSettingsManager.getById(storageSettingsName).orElseThrow(() ->
                        new RuntimeException(String.format("Error occurred while rendering page: Missing " +
                                "storage settings with name %d", storageSettingsName)));

                backupProperties.put("Processors", currentBackupProperties.getProcessors().toString());
                backupProperties.put("Stored on", storageSettings.getType().toString());

                WebBackupItem webBackupItem = new WebBackupItem(currentBackupProperties.getId(),
                        backupProperties.toString(),
                        currentBackupProperties.getBackupName(),
                        dateFormat.format(currentBackupProperties.getDate()));
                backupList.add(webBackupItem);
            }

            model.addAttribute("backupList", backupList);
        }

        model.addAttribute("webAddDatabaseRequest", new WebAddDatabaseRequest());
        model.addAttribute("webAddStorageRequest", new WebAddStorageRequest());
        model.addAttribute("webCreateBackupRequest", new WebCreateBackupRequest());
        model.addAttribute("webRestoreBackupRequest", new WebRestoreBackupRequest());
    }

    @RequestMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }
}
