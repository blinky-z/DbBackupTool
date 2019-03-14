package com.example.demo.models;

/**
 * Either monad-like Response model
 * If no error occurred, the response result can be found in <b>body</b> field
 * Otherwise, look at <b>error</b> field containing error message
 */
public class ResponseTransfer {
    private String body;

    private String error;

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
