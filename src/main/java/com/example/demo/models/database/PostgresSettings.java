package com.example.demo.models.database;

import javax.persistence.*;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.Date;
import java.util.HashMap;

@Entity
@Table(name = "postgres_settings")
public class PostgresSettings implements DatabaseSettings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

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

    @Column(insertable = false)
    private Date date;

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

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    @Override
    public DatabaseType getDatabaseType() {
        return DatabaseType.POSTGRES;
    }

    @Override
    public HashMap<String, String> getProperties() {
        HashMap<String, String> properties = new HashMap<>();

        properties.put("Host", host);
        properties.put("Port", port);
        properties.put("Name", name);
        properties.put("Login", login);
        properties.put("Password", password);

        return properties;
    }
}
