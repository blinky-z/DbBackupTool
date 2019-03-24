package com.example.demo.webUI.formTransfer;

import com.example.demo.webUI.formTransfer.database.WebPostgresSettings;

public class WebAddDatabaseRequest {
    private String databaseType;

    private String host;

    private String port;

    private String name;

    private String login;

    private String password;

    private WebPostgresSettings postgresSettings;

    public String getDatabaseType() {
        return databaseType;
    }

    public void setDatabaseType(String databaseType) {
        this.databaseType = databaseType;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public WebPostgresSettings getPostgresSettings() {
        return postgresSettings;
    }

    public void setPostgresSettings(WebPostgresSettings postgresSettings) {
        this.postgresSettings = postgresSettings;
    }
}
