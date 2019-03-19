package com.example.demo;

import com.example.demo.WebUiSettings.CreateBackupSettings;
import com.example.demo.WebUiSettings.CreateDatabaseSettings;
import com.example.demo.WebUiSettings.CreateStorageSettings;
import com.example.demo.WebUiSettings.DatabaseSettings;
import com.example.demo.settings.UserSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.validation.Valid;

@Controller
public class ApiController {
    private UserSettings userSettings;

    private JdbcTemplate jdbcTemplate;

    private static final Logger logger = LoggerFactory.getLogger(DbBackup.class);

    @Autowired
    public void setUserSettings(UserSettings userSettings) {
        this.userSettings = userSettings;
    }

    @Autowired
    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @DeleteMapping(value = "/database")
    public String deleteDatabase(@RequestParam(value = "databaseType", required = true) String databaseType,
                                         @RequestParam(value = "id", required = true) long id) {
        switch (databaseType) {
            case "postgres": {
                jdbcTemplate.update("delete from postgres_settings where id=?", id);
            }
        }

        return "redirect:/dashboard";
    }

    @PostMapping(value = "/database")
    public String createDatabase(@Valid CreateDatabaseSettings createDatabaseSettings, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            logger.info("Has errors");
            logger.info(bindingResult.getAllErrors().toString());
        }

        String databaseType = createDatabaseSettings.getDatabaseType();
        switch (databaseType) {
            case "postgres": {
                DatabaseSettings databaseSettings = createDatabaseSettings.getDatabaseSettings();
                jdbcTemplate.update("insert into postgres_settings (host, port, name, login, password) values(?, ?, ?, ?, ?)",
                        databaseSettings.getHost(), databaseSettings.getPort(), databaseSettings.getName(), databaseSettings.getLogin(),
                        databaseSettings.getPassword());
            }
        }

        return "redirect:/dashboard";
    }

    @DeleteMapping(value = "/storage")
    public String deleteStorage(@RequestParam(value = "storageType", required = true) String storageType,
                                        @RequestParam(value = "id", required = true) long id) {
        logger.info("Deletion of storage: storage type: {}, id: {}", storageType, id);

        switch (storageType) {
            case "dropbox": {
                jdbcTemplate.update("delete from dropbox_settings where id=?", id);
            }
            case "localFileSystem": {
                jdbcTemplate.update("delete from local_file_system_settings where id=?", id);
            }
        }

        return "redirect:/dashboard";
    }

    @PostMapping(value = "/storage")
    public String createStorage(@Valid CreateStorageSettings createStorageSettings,
                                        BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            logger.info("Has errors");
            logger.info(bindingResult.getAllErrors().toString());
        }

        String storageType = createStorageSettings.getStorageType();
        switch (storageType) {
            case "dropbox": {
                jdbcTemplate.update("insert into dropbox_settings (access_token) values(?)",
                        createStorageSettings.getDropboxSettings().getAccessToken());
            }
            case "localFileSystem": {
                jdbcTemplate.update("insert into local_file_system_settings (backup_path) values(?)",
                        createStorageSettings.getLocalFileSystemSettings().getBackupPath());
            }
        }

        return "redirect:/dashboard";
    }

    @PostMapping(value = "/api/create-backup")
    public ResponseEntity createBackup(@Valid CreateBackupSettings createBackupSettings, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            logger.info("Has errors");
            logger.info(bindingResult.getAllErrors().toString());
        }

        logger.info("{}", createBackupSettings.getDatabaseType());

        logger.info("{}", createBackupSettings.getDatabaseSettings().getHost());
        logger.info("{}", createBackupSettings.getDatabaseSettings().getPort());
        logger.info("{}", createBackupSettings.getDatabaseSettings().getName());
        logger.info("{}", createBackupSettings.getDatabaseSettings().getLogin());
        logger.info("{}", createBackupSettings.getDatabaseSettings().getPassword());

        return ResponseEntity.status(HttpStatus.OK).body(null);
    }
}
