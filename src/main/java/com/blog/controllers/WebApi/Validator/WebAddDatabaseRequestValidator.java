package com.blog.controllers.WebApi.Validator;

import com.blog.controllers.Errors.ValidationException;
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
    void validateDatabaseSpecificFields(Object target, Errors errors) {
        WebAddDatabaseRequest webAddDatabaseRequest = (WebAddDatabaseRequest) target;

        Optional<DatabaseType> optionalDatabaseType = DatabaseType.of(webAddDatabaseRequest.getDatabaseType());
        if (!optionalDatabaseType.isPresent()) {
            errors.rejectValue("databaseType", "error.addDatabaseRequest.databaseType.malformed");
            return;
        }

        DatabaseType databaseType = optionalDatabaseType.get();
        switch (databaseType) {
            case POSTGRES: {
                break;
            }
            default: {
                throw new ValidationException("Unknown database type: " + databaseType);
            }
        }
    }

    public void validate(@NotNull Object target, @NotNull Errors errors) throws ValidationException {
        Objects.requireNonNull(target);
        Objects.requireNonNull(errors);

        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "databaseType", "error.addDatabaseRequest.databaseType.empty");

        WebAddDatabaseRequest webAddDatabaseRequest = (WebAddDatabaseRequest) target;

        // validate common fields
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "settingsName", "error.addDatabaseRequest.settingsName.empty");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "host", "error.addDatabaseRequest.host.empty");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "port", "error.addDatabaseRequest.port.empty");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "databaseName", "error.addDatabaseRequest.databaseName.empty");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "login", "error.addDatabaseRequest.login.empty");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "password", "error.addDatabaseRequest.password.empty");

        if (!errors.hasFieldErrors("port")) {
            try {
                String port = webAddDatabaseRequest.getPort();
                Integer.valueOf(port);
            } catch (NumberFormatException ex) {
                errors.rejectValue("port", "error.addDatabaseRequest.port.malformed");
            }
        }

        // validate database specific fields
        if (!errors.hasFieldErrors("databaseType")) {
            validateDatabaseSpecificFields(target, errors);
        }
    }
}
