package com.example.demo.entities.database;

import org.jetbrains.annotations.NotNull;

import javax.persistence.*;
import java.util.Date;
import java.util.Optional;

@Entity
@Table(name = "database_settings")
public class DatabaseSettings {
    @Id
    @Column(insertable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Enumerated(EnumType.STRING)
    @Column(updatable = false)
    private Database type;

    private final String host;

    private final String port;

    private final String name;

    private final String login;

    private final String password;

    @Column(insertable = false, updatable = false)
    private Date date;

    @Column(name = "additionalFields")
    @Convert(converter = AdditionalDatabaseSettingsConverter.class)
    private AdditionalDatabaseSettings additionalDatabaseSettings;

    public int getId() {
        return id;
    }

    public Database getType() {
        return type;
    }

    public String getHost() {
        return host;
    }

    public String getPort() {
        return port;
    }

    public String getName() {
        return name;
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    public Date getDate() {
        return date;
    }

    public Optional<PostgresSettings> getPostgresSettings() {
        return Optional.ofNullable(additionalDatabaseSettings.getPostgresSettings());
    }

    private DatabaseSettings(Database type, String host, String port, String name, String login, String password, AdditionalDatabaseSettings additionalDatabaseSettings) {
        this.type = type;
        this.host = host;
        this.port = port;
        this.name = name;
        this.login = login;
        this.password = password;
        this.additionalDatabaseSettings = additionalDatabaseSettings;
    }

    @Override
    public String toString() {
        return "DatabaseSettings{" +
                "id=" + id +
                ", type=" + type +
                ", host='" + host + '\'' +
                ", port='" + port + '\'' +
                ", name='" + name + '\'' +
                ", login='" + login + '\'' +
                ", password='" + password + '\'' +
                ", date=" + date +
                '}';
    }

    private static final class Builder {
        private Database type;
        private String host;
        private String port;
        private String name;
        private String login;
        private String password;
        private PostgresSettings postgresSettings;

        private Builder() {
        }

        public DatabaseSettings build() {
            AdditionalDatabaseSettings additionalDatabaseSettings = new AdditionalDatabaseSettings(type, postgresSettings);
            return new DatabaseSettings(type, host, port, name, login, password, additionalDatabaseSettings);
        }
    }

    public static Builder postgresSettings(@NotNull PostgresSettings postgresSettings, String host, String port, String name, String login, String password) {
        Builder builder = new Builder();
        builder.type = Database.POSTGRES;
        builder.host = host;
        builder.port = port;
        builder.name = name;
        builder.login = login;
        builder.password = password;
        builder.postgresSettings = postgresSettings;
        return builder;
    }
}
