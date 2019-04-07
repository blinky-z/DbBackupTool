package com.example.demo.controllers.Errors;

public class DataAccessUserError extends RuntimeException {
    public DataAccessUserError(String error) {
        super(error);
    }
}
