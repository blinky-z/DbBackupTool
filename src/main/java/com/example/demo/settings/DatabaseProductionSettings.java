package com.example.demo.settings;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("production")
@Qualifier(value = "db-settings")
@Component
@ConfigurationProperties(prefix = "spring.datasource")
public class DatabaseProductionSettings implements DatabaseSettings {
    private String url;

    private String username;

    private String password;

    public void setUrl(String url) {
        this.url = url;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }
}
