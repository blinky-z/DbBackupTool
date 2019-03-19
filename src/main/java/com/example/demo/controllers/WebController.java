package com.example.demo.controllers;

import com.example.demo.models.database.DatabaseSettings;
import com.example.demo.models.storage.StorageSettings;
import com.example.demo.repositories.database.PostgresSettingsDatabaseRepository;
import com.example.demo.repositories.storage.DropboxSettingsStorageRepository;
import com.example.demo.repositories.storage.LocalFileSystemSettingsStorageRepository;
import com.example.demo.webUi.webUiFrontModels.DatabaseItem;
import com.example.demo.webUi.webUiFrontModels.StorageItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

@Controller
public class WebController {
    private LocalFileSystemSettingsStorageRepository localFileSystemSettingsStorageRepository;

    private DropboxSettingsStorageRepository dropboxSettingsStorageRepository;

    private PostgresSettingsDatabaseRepository postgresSettingsDatabaseRepository;

    @Autowired
    public void setLocalFileSystemSettingsStorageRepository(LocalFileSystemSettingsStorageRepository localFileSystemSettingsStorageRepository) {
        this.localFileSystemSettingsStorageRepository = localFileSystemSettingsStorageRepository;
    }

    @Autowired
    public void setDropboxSettingsStorageRepository(DropboxSettingsStorageRepository dropboxSettingsStorageRepository) {
        this.dropboxSettingsStorageRepository = dropboxSettingsStorageRepository;
    }

    @Autowired
    public void setPostgresSettingsDatabaseRepository(PostgresSettingsDatabaseRepository postgresSettingsDatabaseRepository) {
        this.postgresSettingsDatabaseRepository = postgresSettingsDatabaseRepository;
    }

    @RequestMapping("/")
    public String index() {
        return "index.html";
    }

    @RequestMapping("/login")
    public String login() {
        return "login.html";
    }

    @RequestMapping("/dashboard")
    public String dashboard(Model model) {
        List<StorageItem> storageList = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

        for (StorageSettings currentStorageSettings : localFileSystemSettingsStorageRepository.findAll()) {
            StorageItem storageItem = new StorageItem(currentStorageSettings.getStorageType(), currentStorageSettings.getId(),
                    currentStorageSettings.getProperties().toString(), dateFormat.format(currentStorageSettings.getDate()));
            storageList.add(storageItem);
        }
        for (StorageSettings currentStorageSettings : dropboxSettingsStorageRepository.findAll()) {
            StorageItem storageItem = new StorageItem(currentStorageSettings.getStorageType(), currentStorageSettings.getId(),
                    currentStorageSettings.getProperties().toString(), dateFormat.format(currentStorageSettings.getDate()));
            storageList.add(storageItem);
        }

        List<DatabaseItem> databaseList = new ArrayList<>();
        for (DatabaseSettings currentStorageSettings : postgresSettingsDatabaseRepository.findAll()) {
            DatabaseItem databaseItem = new DatabaseItem(currentStorageSettings.getDatabaseType(), currentStorageSettings.getId(),
                    currentStorageSettings.getProperties().toString(), dateFormat.format(currentStorageSettings.getDate()));
            databaseList.add(databaseItem);
        }

        model.addAttribute("storageList", storageList);
        model.addAttribute("databaseList", databaseList);

        return "dashboard.html";
    }
}
