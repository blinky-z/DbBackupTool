package com.blog.controllers.WebApi.Validator;

import com.blog.entities.database.DatabaseType;
import com.blog.webUI.formTransfer.WebAddDatabaseRequest;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;
import org.springframework.validation.Validator;

import java.util.Optional;

@Component
public class WebAddDatabaseRequestValidator implements Validator {
    @Override
    public boolean supports(@NotNull Class<?> clazz) {
        return WebAddDatabaseRequest.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(@NotNull Object target, @NotNull Errors errors) {
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "databaseType", "error.addDatabaseRequest.databaseType.empty",
                "Please specify database type");

        WebAddDatabaseRequest webAddDatabaseRequest = (WebAddDatabaseRequest) target;

        Optional<DatabaseType> optionalDatabaseType = DatabaseType.of(webAddDatabaseRequest.getDatabaseType());
        if (optionalDatabaseType.isPresent()) {
            // validate common fields
            ValidationUtils.rejectIfEmptyOrWhitespace(errors, "settingsName",
                    "error.addDatabaseRequest.settingsName.empty", "Please provide settings name");
            ValidationUtils.rejectIfEmptyOrWhitespace(errors, "host",
                    "error.addDatabaseRequest.host.empty", "Please provide host");
            ValidationUtils.rejectIfEmptyOrWhitespace(errors, "port",
                    "error.addDatabaseRequest.port.empty", "Please provide port");
            ValidationUtils.rejectIfEmptyOrWhitespace(errors, "databaseName",
                    "error.addDatabaseRequest.databaseName.empty", "Please provide database name");
            ValidationUtils.rejectIfEmptyOrWhitespace(errors, "login",
                    "error.addDatabaseRequest.login.empty", "Please provide login");
            ValidationUtils.rejectIfEmptyOrWhitespace(errors, "password",
                    "error.addDatabaseRequest.password.empty", "Please provide password");

            String port = webAddDatabaseRequest.getPort();
            if (port != null && !port.isEmpty()) {
                try {
                    Integer.valueOf(port);
                } catch (NumberFormatException ex) {
                    errors.rejectValue("port", "error.addDatabaseRequest.port.malformed",
                            "Invalid port. Must be only number");
                }
            }

            // validate database type specific fields
            DatabaseType databaseType = optionalDatabaseType.get();
            switch (databaseType) {
                case POSTGRES: {
                    break;
                }
            }
        } else {
            errors.rejectValue("databaseType", "error.addDatabaseRequest.databaseType.malformed",
                    "Please provide proper database type");
        }
    }
}
