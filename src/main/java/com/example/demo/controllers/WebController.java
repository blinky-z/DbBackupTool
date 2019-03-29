package com.example.demo.controllers;

import com.example.demo.entities.backup.BackupProperties;
import com.example.demo.entities.database.PostgresSettings;
import com.example.demo.manager.BackupPropertiesManager;
import com.example.demo.manager.DatabaseSettingsManager;
import com.example.demo.manager.StorageSettingsManager;
import com.example.demo.entities.database.Database;
import com.example.demo.entities.database.DatabaseSettings;
import com.example.demo.entities.storage.DropboxSettings;
import com.example.demo.entities.storage.LocalFileSystemSettings;
import com.example.demo.entities.storage.Storage;
import com.example.demo.entities.storage.StorageSettings;
import com.example.demo.webUI.renderModels.WebBackupItem;
import com.example.demo.webUI.renderModels.WebDatabaseItem;
import com.example.demo.webUI.renderModels.WebStorageItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Controller
public class WebController {
    private static final Logger logger = LoggerFactory.getLogger(WebController.class);

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
        return "login.html";
    }

    @RequestMapping("/dashboard")
    public String dashboard(Model model) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(TIME_FORMAT);

        {
            List<WebStorageItem> storageList = new ArrayList<>();

            for (StorageSettings storageSettings : storageSettingsManager.getAllByType(Storage.LOCAL_FILE_SYSTEM)) {
                LocalFileSystemSettings localFileSystemSettings = storageSettings.getLocalFileSystemSettings().orElseThrow(
                        RuntimeException::new);

                HashMap<String, String> storageProperties = new HashMap<>();
                storageProperties.put("Backup path", localFileSystemSettings.getBackupPath());

                WebStorageItem storageItem = new WebStorageItem(storageSettings.getType(), storageSettings.getId(),
                        storageProperties.toString(), dateFormat.format(storageSettings.getDate()));
                storageList.add(storageItem);
            }
            for (StorageSettings storageSettings : storageSettingsManager.getAllByType(Storage.DROPBOX)) {
                DropboxSettings dropboxSettings = storageSettings.getDropboxSettings().orElseThrow(RuntimeException::new);

                HashMap<String, String> storageProperties = new HashMap<>();
                storageProperties.put("Access token", dropboxSettings.getAccessToken());

                WebStorageItem storageItem = new WebStorageItem(storageSettings.getType(), storageSettings.getId(),
                        storageProperties.toString(), dateFormat.format(storageSettings.getDate()));
                storageList.add(storageItem);
            }

            model.addAttribute("storageList", storageList);
        }

        {
            List<WebDatabaseItem> databaseList = new ArrayList<>();

            for (DatabaseSettings databaseSettings : databaseSettingsManager.getAllByType(Database.POSTGRES)) {
                PostgresSettings postgresSettings = databaseSettings.getPostgresSettings().orElseThrow(RuntimeException::new);

                HashMap<String, String> storageProperties = new HashMap<>();
                storageProperties.put("Host", databaseSettings.getHost());
                storageProperties.put("Port", Integer.toString(databaseSettings.getPort()));
                storageProperties.put("Database name", databaseSettings.getName());

                WebDatabaseItem databaseItem = new WebDatabaseItem(databaseSettings.getType(), databaseSettings.getId(),
                        storageProperties.toString(), dateFormat.format(databaseSettings.getDate()));
                databaseList.add(databaseItem);
            }

            model.addAttribute("databaseList", databaseList);
        }

        {
            List<WebBackupItem> backupList = new ArrayList<>();

            for (BackupProperties currentBackupProperties : backupPropertiesManager.getAll()) {
                HashMap<String, String> backupProperties = new HashMap<>();

                Integer storageSettingsId = currentBackupProperties.getStorageSettingsId();
                StorageSettings storageSettings = storageSettingsManager.getById(storageSettingsId).orElseThrow(() ->
                        new RuntimeException(String.format("Error occurred while rendering page: Missing " +
                                "storage settings with ID %d", storageSettingsId)));

                backupProperties.put("Compressed", currentBackupProperties.isCompressed().toString());
                backupProperties.put("Stored on", storageSettings.getType().toString());

                WebBackupItem webBackupItem = new WebBackupItem(currentBackupProperties.getId(),
                        backupProperties.toString(),
                        currentBackupProperties.getBackupName(),
                        dateFormat.format(currentBackupProperties.getDate()));
                backupList.add(webBackupItem);
            }

            model.addAttribute("backupList", backupList);
        }

        return "dashboard.html";
    }
}
