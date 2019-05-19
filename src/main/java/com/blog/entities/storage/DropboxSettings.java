package com.blog.entities.storage;

/**
 * This class represents Dropbox specific properties.
 */
public class DropboxSettings {
    private String accessToken;

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    @Override
    public String toString() {
        return "DropboxSettings{" +
                "accessToken='" + accessToken + '\'' +
                '}';
    }
}
