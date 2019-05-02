package com.blog.controllers.Errors;


/**
 * Error that occurs when working with storage or database settings repository
 */
public class DataAccessError extends RuntimeException {
    public DataAccessError(String error) {
        super(error);
    }
}
