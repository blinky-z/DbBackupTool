package com.example.demo.controllers.WebApi;

import com.example.demo.controllers.Errors.DataAccessUserError;
import com.example.demo.controllers.Errors.ValidationError;
import com.example.demo.controllers.WebApi.Validator.WebAddDatabaseRequestValidator;
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
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
@RequestMapping("/database")
public class WebApiDatabaseController {
    private static final Logger logger = LoggerFactory.getLogger(WebApiDatabaseController.class);

    private DatabaseSettingsManager databaseSettingsManager;

    private WebAddDatabaseRequestValidator webAddDatabaseRequestValidator;

    @Autowired
    public void setDatabaseSettingsManager(DatabaseSettingsManager databaseSettingsManager) {
        this.databaseSettingsManager = databaseSettingsManager;
    }

    @Autowired
    public void setWebAddDatabaseRequestValidator(WebAddDatabaseRequestValidator webAddDatabaseRequestValidator) {
        this.webAddDatabaseRequestValidator = webAddDatabaseRequestValidator;
    }

    private String validateDeleteDatabaseRequest(Optional<String> optionalSettingsName) {
        if (!optionalSettingsName.isPresent()) {
            return "Please, provide database settings name to delete";
        }

        String settingsName = optionalSettingsName.get();
        if (settingsName.isEmpty()) {
            return "Please, provide database settings name to delete";
        }

        return "";
    }

    @DeleteMapping
    public String deleteDatabase(@RequestParam(value = "settingsName") Optional<String> optionalSettingsName) {
        String error = validateDeleteDatabaseRequest(optionalSettingsName);
        if (!error.isEmpty()) {
            throw new ValidationError(error);
        }

        String settingsName = optionalSettingsName.get();

        logger.info("deleteDatabase(): Got database settings deletion job. Settings name: {}", settingsName);

        try {
            databaseSettingsManager.deleteById(settingsName);
        } catch (NonTransientDataAccessException ignored) {

        }

        return "redirect:/dashboard";
    }

    @PostMapping
    public String createDatabase(WebAddDatabaseRequest addDatabaseRequest, BindingResult bindingResult) {
        logger.info("createDatabase(): Got database configuration creation job");

        webAddDatabaseRequestValidator.validate(addDatabaseRequest, bindingResult);
        if (bindingResult.hasErrors()) {
            logger.info("Has errors: {}", bindingResult.getAllErrors());

            return "dashboard";
        }

        String settingsName = addDatabaseRequest.getSettingsName();
        if (databaseSettingsManager.existsById(settingsName)) {
            throw new DataAccessUserError("Database settings with name '" + settingsName + "' already exists");
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
