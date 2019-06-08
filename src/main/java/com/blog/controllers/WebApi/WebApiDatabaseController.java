package com.blog.controllers.WebApi;

import com.blog.controllers.Errors.ValidationException;
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

import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
        logger.info("createDatabase(): Got database settings creation request");

        webAddDatabaseRequestValidator.validate(addDatabaseRequest, bindingResult);
        if (bindingResult.hasErrors()) {
            logger.error("Invalid database settings creation request. Errors: {}", bindingResult.getAllErrors());

            return "dashboard";
        }

        Optional<DatabaseType> databaseType = DatabaseType.of(addDatabaseRequest.getDatabaseType());
        if (!databaseType.isPresent()) {
            throw new IllegalStateException("Can't create database settings: Invalid database type");
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
                        .withDate(LocalDateTime.now(ZoneOffset.UTC))
                        .build();
                break;
            }
            default: {
                throw new IllegalStateException("Can't create database settings: Unknown database type" + databaseType.get());
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
        logger.info("deleteDatabase(): Got database settings deletion request");

        String error = validateDeleteDatabaseRequest(optionalSettingsName.orElse(null));
        if (error != null) {
            logger.error("Invalid database settings deletion request. Error: {}", error);

            throw new ValidationException(error);
        }

        String settingsName = optionalSettingsName.get();

        databaseSettingsManager.deleteById(settingsName);

        logger.info("deleteDatabase(): Database settings with name deleted: {}", settingsName);

        return "redirect:/dashboard";
    }
}
