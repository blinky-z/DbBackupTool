package com.blog.controllers.WebApi.Validator;

import com.blog.webUI.formTransfer.WebAddPlannedTaskRequest;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;

import java.util.Objects;

/**
 * Validates planned task creation request.
 *
 * @see com.blog.controllers.WebApi.WebApiTaskController#addPlannedTask(WebAddPlannedTaskRequest, BindingResult)
 */
@Component
public class WebAddPlannedTaskRequestValidator {
    public void validate(@NotNull Object target, @NotNull Errors errors) {
        Objects.requireNonNull(target);
        Objects.requireNonNull(errors);

        WebAddPlannedTaskRequest webAddPlannedTaskRequest = (WebAddPlannedTaskRequest) target;

        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "databaseSettingsName",
                "error.addPlannedTaskRequest.databaseSettingsName.empty");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "storageSettingsNameList",
                "error.addPlannedTaskRequest.storageSettingsNameList.empty");
        ValidationUtils.rejectIfEmptyOrWhitespace(errors, "interval",
                "error.addPlannedTaskRequest.interval.empty");

        if (!errors.hasFieldErrors("interval")) {
            try {
                String interval = webAddPlannedTaskRequest.getInterval();
                Integer.valueOf(interval);
            } catch (NumberFormatException ex) {
                errors.rejectValue("interval", "error.addPlannedTaskRequest.interval.malformed");
            }
        }
    }
}
