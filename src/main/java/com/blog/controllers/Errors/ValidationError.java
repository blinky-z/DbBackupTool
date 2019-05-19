package com.blog.controllers.Errors;

/**
 * This exception is thrown when request validation failed and request was not sent by input form (e.g. deletion request).
 */
public class ValidationError extends RuntimeException {
    public ValidationError(String error) {
        super(error);
    }
}
