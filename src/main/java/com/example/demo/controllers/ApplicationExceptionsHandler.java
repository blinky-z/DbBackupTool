package com.example.demo.controllers;

import com.example.demo.controllers.WebApi.Errors.ValidationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;

@ControllerAdvice
public class ApplicationExceptionsHandler {
    private static final Logger logger = LoggerFactory.getLogger(ApplicationExceptionsHandler.class);

    private static final String ERROR_CODE_RENDER_FIELD = "errorCode";

    private static final String ERROR_MESSAGE_RENDER_FIELD = "errorMessage";

    private static final String ERROR_VIEW = "error";

    @ExceptionHandler(value = {ValidationError.class})
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    public ModelAndView handleValidationError(HttpServletRequest request, ValidationError ex) {
        logger.error("Validation Exception at request {} : {}", request.getRequestURL(), ex.getMessage());

        ModelAndView mav = new ModelAndView();
        mav.addObject(ERROR_CODE_RENDER_FIELD, HttpStatus.BAD_REQUEST.value());
        mav.addObject(ERROR_MESSAGE_RENDER_FIELD, ex.getMessage());
        mav.setViewName(ERROR_VIEW);
        return mav;
    }

    @ExceptionHandler(value = {RuntimeException.class})
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    public ModelAndView handleRuntimeException(HttpServletRequest request, RuntimeException ex) {
        logger.error("Runtime Exception at request {}", request.getRequestURL(), ex);

        ModelAndView mav = new ModelAndView();
        mav.addObject(ERROR_CODE_RENDER_FIELD, HttpStatus.INTERNAL_SERVER_ERROR.value());
        mav.addObject(ERROR_MESSAGE_RENDER_FIELD, ex.getMessage());
        mav.setViewName(ERROR_VIEW);
        return mav;
    }
}
