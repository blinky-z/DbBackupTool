package com.example.demo.entities.database;

import org.jetbrains.annotations.NotNull;

import javax.persistence.*;
import java.util.Date;
import java.util.Objects;
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

    private DatabaseSettings(@NotNull Database type, @NotNull String host, @NotNull String port, @NotNull String name,
                             @NotNull String login, @NotNull String password,
                             @NotNull AdditionalDatabaseSettings additionalDatabaseSettings) {
        this.type = Objects.requireNonNull(type);
        this.host = Objects.requireNonNull(host);
        this.port = Objects.requireNonNull(port);
        this.name = Objects.requireNonNull(name);
        this.login = Objects.requireNonNull(login);
        this.password = Objects.requireNonNull(password);
        this.additionalDatabaseSettings = Objects.requireNonNull(additionalDatabaseSettings);
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

    public static final class Builder {
        private Database type;
        private String host;
        private String port;
        private String name;
        private String login;
        private String password;
        private PostgresSettings postgresSettings;

        private Builder() {
        }

        public Builder withHost(@NotNull String host) {
            this.host = Objects.requireNonNull(host);
            return this;
        }

        public Builder withPort(@NotNull String port) {
            this.port = Objects.requireNonNull(port);
            return this;
        }

        public Builder withName(@NotNull String name) {
            this.name = Objects.requireNonNull(name);
            return this;
        }

        public Builder withLogin(@NotNull String login) {
            this.login = Objects.requireNonNull(login);
            return this;
        }

        public Builder withPassword(@NotNull String password) {
            this.password = Objects.requireNonNull(password);
            return this;
        }

        public DatabaseSettings build() {
            AdditionalDatabaseSettings additionalDatabaseSettings = new AdditionalDatabaseSettings(type, postgresSettings);
            return new DatabaseSettings(type, host, port, name, login, password, additionalDatabaseSettings);
        }
    }

    public static Builder postgresSettings(@NotNull PostgresSettings postgresSettings) {
        Builder builder = new Builder();
        builder.type = Database.POSTGRES;
        builder.postgresSettings = Objects.requireNonNull(postgresSettings);
        return builder;
    }
}
