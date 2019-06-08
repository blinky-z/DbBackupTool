package com.blog.controllers;

import com.blog.controllers.Errors.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;

@ControllerAdvice
public class WebApplicationExceptionsHandler {
    private static final Logger logger = LoggerFactory.getLogger(WebApplicationExceptionsHandler.class);

    private static final String ERROR_PAGE_CODE_FIELD = "errorCode";

    private static final String ERROR_PAGE_MESSAGE_FIELD = "errorMessage";

    private static final String ERROR_VIEW = "error";

    @ExceptionHandler(value = {ValidationException.class})
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    public ModelAndView handleValidationError(HttpServletRequest request, ValidationException ex) {
        logger.error("Validation Error Exception at request {} : {}", request.getRequestURL(), ex.getMessage());

        ModelAndView mav = new ModelAndView();
        mav.addObject(ERROR_PAGE_CODE_FIELD, HttpStatus.BAD_REQUEST.value());
        mav.addObject(ERROR_PAGE_MESSAGE_FIELD, ex.getMessage());
        mav.setViewName(ERROR_VIEW);
        return mav;
    }

    @ExceptionHandler(value = {IllegalStateException.class})
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    public ModelAndView handleIllegalStateException(HttpServletRequest request, RuntimeException ex) {
        logger.error("Illegal State Exception at request {}", request.getRequestURL(), ex);

        ModelAndView mav = new ModelAndView();
        mav.addObject(ERROR_PAGE_CODE_FIELD, HttpStatus.INTERNAL_SERVER_ERROR.value());
        mav.addObject(ERROR_PAGE_MESSAGE_FIELD, ex.getMessage());
        mav.setViewName(ERROR_VIEW);
        return mav;
    }
}
