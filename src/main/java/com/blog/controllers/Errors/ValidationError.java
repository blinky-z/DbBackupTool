package com.blog.controllers.Errors;

public class ValidationError extends RuntimeException {
    public ValidationError(String error) {
        super(error);
    }
}
