package com.blog.webUI.formTransfer.storage;

/**
 * This class stores Dropbox storage specific fields
 */
public class WebDropboxSettings {
    private String accessToken;

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    @Override
    public String toString() {
        return "WebDropboxSettings{" +
                "accessToken='" + accessToken + '\'' +
                '}';
    }
}
