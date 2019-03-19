package com.example.demo.WebUiSettings;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

public class DatabaseSettings {
    @Valid
    @NotEmpty
    private String host;

    @Valid
    @NotEmpty
    private String port;

    @Valid
    @NotEmpty
    private String name;

    @Valid
    @NotEmpty
    private String login;

    @Valid
    @NotEmpty
    private String password;

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
}
