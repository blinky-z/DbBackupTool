package com.example.demo.controllers;

import com.example.demo.DatabaseManager.DatabaseManager;
import com.example.demo.StorageManager.StorageManager;
import com.example.demo.entities.database.Database;
import com.example.demo.entities.database.DatabaseSettings;
import com.example.demo.entities.storage.DropboxSettings;
import com.example.demo.entities.storage.LocalFileSystemSettings;
import com.example.demo.entities.storage.Storage;
import com.example.demo.entities.storage.StorageSettings;
import com.example.demo.webUi.webUiFrontModels.DatabaseItem;
import com.example.demo.webUi.webUiFrontModels.StorageItem;
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
    private static final String TIME_FORMAT = "dd.MM.yyyy HH:mm:ss";

    private DatabaseManager databaseManager;

    private StorageManager storageManager;

    @Autowired
    public void setDatabaseManager(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Autowired
    public void setStorageManager(StorageManager storageManager) {
        this.storageManager = storageManager;
    }

    @RequestMapping("/")
    public String index() {
        return "index.html";
    }

    @RequestMapping("/login")
    public String login() {
        return "login.html";
    }

    //    TODO: сделать добавление всех стореджей в одном цикле, и добавление всех баз данных в одном цикле. Сейчас есть дупликация кода
    @RequestMapping("/dashboard")
    public String dashboard(Model model) {
        List<StorageItem> storageList = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat(TIME_FORMAT);

        for (StorageSettings storageSettings : storageManager.getAllByType(Storage.LOCAL_FILE_SYSTEM)) {
            LocalFileSystemSettings localFileSystemSettings = storageSettings.getLocalFileSystemSettings().get();

            HashMap<String, String> storageProperties = new HashMap<>();
            storageProperties.put("Backup path", localFileSystemSettings.getBackupPath());

            StorageItem storageItem = new StorageItem(storageSettings.getType(), storageSettings.getId(),
                    storageProperties.toString(), dateFormat.format(storageSettings.getDate()));
            storageList.add(storageItem);
        }
        for (StorageSettings storageSettings : storageManager.getAllByType(Storage.DROPBOX)) {
            DropboxSettings dropboxSettings = storageSettings.getDropboxSettings().get();

            HashMap<String, String> storageProperties = new HashMap<>();
            storageProperties.put("Access token", dropboxSettings.getAccessToken());

            StorageItem storageItem = new StorageItem(storageSettings.getType(), storageSettings.getId(),
                    storageProperties.toString(), dateFormat.format(storageSettings.getDate()));
            storageList.add(storageItem);
        }

        List<DatabaseItem> databaseList = new ArrayList<>();
        for (DatabaseSettings databaseSettings : databaseManager.getAllByType(Database.POSTGRES)) {

            HashMap<String, String> storageProperties = new HashMap<>();
            storageProperties.put("Host", databaseSettings.getHost());
            storageProperties.put("Port", databaseSettings.getPort());
            storageProperties.put("Database name", databaseSettings.getName());

            DatabaseItem databaseItem = new DatabaseItem(databaseSettings.getType(), databaseSettings.getId(),
                    storageProperties.toString(), dateFormat.format(databaseSettings.getDate()));
            databaseList.add(databaseItem);
        }

        model.addAttribute("storageList", storageList);
        model.addAttribute("databaseList", databaseList);

        return "dashboard.html";
    }
}
