package com.blog.controllers.WebApi.Validator;

import com.blog.entities.database.DatabaseType;
import com.blog.webUI.formTransfer.WebAddDatabaseRequest;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;

import java.util.Objects;
import java.util.Optional;

/**
 * Validates database settings creation request
 *
 * @see com.blog.controllers.WebApi.WebApiDatabaseController#createDatabase(WebAddDatabaseRequest, BindingResult)
 */
@Component
public class WebAddDatabaseRequestValidator {
    public void validate(@NotNull Object target, @NotNull Errors errors) {
        Objects.requireNonNull(target);
        Objects.requireNonNull(errors);

        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "databaseType",
                "error.addDatabaseRequest.databaseType.empty", "Please specify database type");

        WebAddDatabaseRequest webAddDatabaseRequest = (WebAddDatabaseRequest) target;

        Optional<DatabaseType> optionalDatabaseType = DatabaseType.of(webAddDatabaseRequest.getDatabaseType());
        if (!optionalDatabaseType.isPresent()) {
            errors.rejectValue("databaseType", "error.addDatabaseRequest.databaseType.malformed",
                    "Invalid database type");
        }

        // validate common fields
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "settingsName",
                "error.addDatabaseRequest.settingsName.empty", "Settings name must not be empty");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "host",
                "error.addDatabaseRequest.host.empty", "Host must not be empty");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "port",
                "error.addDatabaseRequest.port.empty", "Port must not be empty");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "databaseName",
                "error.addDatabaseRequest.databaseName.empty", "Database name must not be empty");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "login",
                "error.addDatabaseRequest.login.empty", "Login must not be empty");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "password",
                "error.addDatabaseRequest.password.empty", "Password must not be empty");

        if (!errors.hasFieldErrors("port")) {
            try {
                String port = webAddDatabaseRequest.getPort();
                Integer.valueOf(port);
            } catch (NumberFormatException ex) {
                errors.rejectValue("port", "error.addDatabaseRequest.port.malformed",
                        "Invalid port");
            }
        }
    }
}
