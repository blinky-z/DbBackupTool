package com.blog.controllers.Errors;

/**
 * Error that occurs when request validation failed
 */
public class ValidationError extends RuntimeException {
    public ValidationError(String error) {
        super(error);
    }
}
