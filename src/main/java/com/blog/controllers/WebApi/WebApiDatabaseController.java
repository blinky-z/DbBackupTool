package com.blog.controllers.WebApi;

import com.blog.controllers.Errors.ValidationError;
import com.blog.controllers.WebApi.Validator.WebAddDatabaseRequestValidator;
import com.blog.entities.database.DatabaseSettings;
import com.blog.entities.database.DatabaseType;
import com.blog.entities.database.PostgresSettings;
import com.blog.manager.DatabaseSettingsManager;
import com.blog.webUI.formTransfer.WebAddDatabaseRequest;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

/**
 * This controller is responsible for database settings handling: creation, deletion of settings.
 */
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

    @PostMapping
    public String createDatabase(WebAddDatabaseRequest addDatabaseRequest, BindingResult bindingResult) {
        logger.info("createDatabase(): Got database configuration creation job");

        webAddDatabaseRequestValidator.validate(addDatabaseRequest, bindingResult);
        if (bindingResult.hasErrors()) {
            return "dashboard";
        }

        Optional<DatabaseType> databaseType = DatabaseType.of(addDatabaseRequest.getDatabaseType());
        if (!databaseType.isPresent()) {
            throw new RuntimeException("Can't create database settings: Invalid database type");
        }

        DatabaseSettings databaseSettings;

        switch (databaseType.get()) {
            case POSTGRES: {
                PostgresSettings postgresSettings = new PostgresSettings();

                databaseSettings = DatabaseSettings.postgresSettings(postgresSettings)
                        .withHost(addDatabaseRequest.getHost())
                        .withPort(Integer.valueOf(addDatabaseRequest.getPort()))
                        .withDatabaseName(addDatabaseRequest.getDatabaseName())
                        .withLogin(addDatabaseRequest.getLogin())
                        .withPassword(addDatabaseRequest.getPassword())
                        .withSettingsName(addDatabaseRequest.getSettingsName())
                        .build();
                break;
            }
            default: {
                throw new RuntimeException("Can't create database settings: Unknown database type" + databaseType.get());
            }
        }

        DatabaseSettings savedDatabaseSettings = databaseSettingsManager.save(databaseSettings);

        logger.info("Database settings saved into database. Saved database settings: {}", savedDatabaseSettings);

        return "redirect:/dashboard";
    }

    @Nullable
    private String validateDeleteDatabaseRequest(@Nullable String settingsName) {
        if (settingsName == null || settingsName.isEmpty() || settingsName.trim().isEmpty()) {
            return "Please, provide database settings name to delete";
        }

        return null;
    }

    @DeleteMapping
    public String deleteDatabase(@RequestParam(value = "settingsName") Optional<String> optionalSettingsName) {
        String error = validateDeleteDatabaseRequest(optionalSettingsName.orElse(null));
        if (error != null) {
            throw new ValidationError(error);
        }

        String settingsName = optionalSettingsName.get();

        logger.info("deleteDatabase(): Got database settings deletion job. Settings name: {}", settingsName);

        databaseSettingsManager.deleteById(settingsName);

        return "redirect:/dashboard";
    }
}
