package com.blog.controllers.Errors;

/**
 * This exception is thrown when request validation failed and request was not sent by html form (e.g. deletion request).
 * <p>
 * This exception is caught by {@link com.blog.controllers.WebApplicationExceptionsHandler}.
 */
public class ValidationException extends RuntimeException {
    public ValidationException(String error) {
        super(error);
    }
}
