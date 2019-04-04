package com.example.demo.controllers.WebApi.Errors;

public class ValidationError extends RuntimeException {
    public ValidationError(String message) {
        super(message);
    }
}