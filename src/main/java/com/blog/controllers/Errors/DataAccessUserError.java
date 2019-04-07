package com.blog.controllers.Errors;

public class DataAccessUserError extends RuntimeException {
    public DataAccessUserError(String error) {
        super(error);
    }
}
