package com.example.demo.controllers.WebApi;

import com.example.demo.controllers.WebApi.Errors.ValidationError;
import com.example.demo.entities.database.DatabaseSettings;
import com.example.demo.entities.database.DatabaseType;
import com.example.demo.entities.database.PostgresSettings;
import com.example.demo.manager.DatabaseSettingsManager;
import com.example.demo.webUI.formTransfer.WebAddDatabaseRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.NonTransientDataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.validation.Valid;
import java.util.Optional;

@Controller
@RequestMapping("/database")
public class WebApiDatabaseController {
    private static final Logger logger = LoggerFactory.getLogger(WebApiDatabaseController.class);

    private DatabaseSettingsManager databaseSettingsManager;

    @Autowired
    public void setDatabaseSettingsManager(DatabaseSettingsManager databaseSettingsManager) {
        this.databaseSettingsManager = databaseSettingsManager;
    }

    @DeleteMapping
    public String deleteDatabase(@RequestParam(value = "settingsName") Optional<String> optionalSettingsName) {
        if (!optionalSettingsName.isPresent()) {
            throw new ValidationError("Please, provide database settings name to delete");
        }

        String settingsName = optionalSettingsName.get();
        if (settingsName.isEmpty()) {
            throw new ValidationError("Please, provide database settings name to delete");
        }

        logger.info("deleteDatabase(): Got database settings deletion job. Settings name: {}", settingsName);

        try {
            databaseSettingsManager.deleteById(settingsName);
        } catch (NonTransientDataAccessException ignored) {

        }

        return "redirect:/dashboard";
    }

    private String validateAddDatabaseRequest(WebAddDatabaseRequest addDatabaseRequest) {
        String databaseTypeAsString = addDatabaseRequest.getDatabaseType();
        if (databaseTypeAsString == null || databaseTypeAsString.isEmpty()) {
            return "Please, specify database type";
        }
        Optional<DatabaseType> optionalDatabaseType = DatabaseType.of(databaseTypeAsString);
        if (!optionalDatabaseType.isPresent()) {
            return "Invalid Database Settings";
        }
        DatabaseType databaseType = optionalDatabaseType.get();
        switch (databaseType) {
            case POSTGRES: {
                break;
            }
        }

        if (addDatabaseRequest.getHost().isEmpty() || addDatabaseRequest.getSettingsName().isEmpty() ||
                addDatabaseRequest.getPort().isEmpty() || addDatabaseRequest.getDatabaseName().isEmpty() ||
                addDatabaseRequest.getLogin().isEmpty() || addDatabaseRequest.getPassword().isEmpty()) {
            return "Invalid database settings";
        }

        try {
            Integer.valueOf(addDatabaseRequest.getPort());
        } catch (NumberFormatException e) {
            return "Invalid database settings";
        }

        return "";
    }

    @PostMapping
    public String createDatabase(@Valid WebAddDatabaseRequest addDatabaseRequest) {
        logger.info("createDatabase(): Got database configuration creation job");

        String error = validateAddDatabaseRequest(addDatabaseRequest);
        if (!error.isEmpty()) {
            throw new ValidationError(error);
        }

        String settingsName = addDatabaseRequest.getSettingsName();
        if (databaseSettingsManager.existsById(settingsName)) {
            throw new ValidationError("Database settings with name '" + settingsName + "' already exists");
        }

        Optional<DatabaseType> databaseType = DatabaseType.of(addDatabaseRequest.getDatabaseType());
        if (databaseType.isPresent()) {
            switch (databaseType.get()) {
                case POSTGRES: {
                    PostgresSettings postgresSettings = new PostgresSettings();

                    DatabaseSettings databaseSettings = DatabaseSettings.postgresSettings(postgresSettings)
                            .withHost(addDatabaseRequest.getHost())
                            .withPort(Integer.valueOf(addDatabaseRequest.getPort()))
                            .withDatabaseName(addDatabaseRequest.getDatabaseName())
                            .withLogin(addDatabaseRequest.getLogin())
                            .withPassword(addDatabaseRequest.getPassword())
                            .withSettingsName(addDatabaseRequest.getSettingsName())
                            .build();
                    databaseSettingsManager.save(databaseSettings);
                    break;
                }
            }
        } else {
            throw new RuntimeException("Can't create database configuration. Error: Unknown database type");
        }

        return "redirect:/dashboard";
    }
}
