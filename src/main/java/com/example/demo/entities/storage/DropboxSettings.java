package com.example.demo.entities.storage;

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
                ", accessToken='" + accessToken + '\'' +
                '}';
    }
}
