package com.blog.entities.database;

import org.jetbrains.annotations.NotNull;

import javax.persistence.*;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;

@Entity
@Table(name = "database_settings")
public class DatabaseSettings {
    @Id
    private String settingsName;

    @Enumerated(EnumType.STRING)
    @Column(updatable = false)
    private DatabaseType type;

    private String host;

    private int port;

    private String name;

    private String login;

    private String password;

    @Column(insertable = false, updatable = false)
    private Date date;

    @Column(name = "additionalFields")
    @Convert(converter = AdditionalDatabaseSettingsConverter.class)
    private AdditionalDatabaseSettings additionalDatabaseSettings;

    DatabaseSettings() {
    }

    private DatabaseSettings(@NotNull DatabaseType type, @NotNull String settingsName, @NotNull String host, int port,
                             @NotNull String name, @NotNull String login, @NotNull String password,
                             @NotNull AdditionalDatabaseSettings additionalDatabaseSettings) {
        this.type = Objects.requireNonNull(type);
        this.settingsName = settingsName;
        this.host = Objects.requireNonNull(host);
        this.port = port;
        this.name = Objects.requireNonNull(name);
        this.login = Objects.requireNonNull(login);
        this.password = Objects.requireNonNull(password);
        this.additionalDatabaseSettings = Objects.requireNonNull(additionalDatabaseSettings);
    }

    public static Builder postgresSettings(@NotNull PostgresSettings postgresSettings) {
        Builder builder = new Builder();
        builder.type = DatabaseType.POSTGRES;
        builder.postgresSettings = Objects.requireNonNull(postgresSettings);
        return builder;
    }

    public DatabaseType getType() {
        return type;
    }

    void setType(DatabaseType type) {
        this.type = type;
    }

    public String getSettingsName() {
        return settingsName;
    }

    public void setSettingsName(String settingsName) {
        this.settingsName = settingsName;
    }

    public String getHost() {
        return host;
    }

    void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    void setPort(int port) {
        this.port = port;
    }

    public String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }

    public String getLogin() {
        return login;
    }

    void setLogin(String login) {
        this.login = login;
    }

    public String getPassword() {
        return password;
    }

    void setPassword(String password) {
        this.password = password;
    }

    public Date getDate() {
        return date;
    }

    void setDate(Date date) {
        this.date = date;
    }

    void setAdditionalDatabaseSettings(AdditionalDatabaseSettings additionalDatabaseSettings) {
        this.additionalDatabaseSettings = additionalDatabaseSettings;
    }

    public Optional<PostgresSettings> getPostgresSettings() {
        return Optional.ofNullable(additionalDatabaseSettings.getPostgresSettings());
    }

    @Override
    public String toString() {
        return "DatabaseSettings{" +
                ", settingsName=" + settingsName +
                ", type=" + type +
                ", host='" + host + '\'' +
                ", port='" + port + '\'' +
                ", databaseName='" + name + '\'' +
                ", login=***'" + '\'' +
                ", password=***'" + '\'' +
                ", date=" + date +
                ", additionalDatabaseSettings=" + additionalDatabaseSettings +
                '}';
    }

    public static final class Builder {
        private DatabaseType type;
        private String settingsName;
        private String host;
        private int port;
        private String databaseName;
        private String login;
        private String password;
        private PostgresSettings postgresSettings;

        private Builder() {
        }

        public Builder withHost(@NotNull String host) {
            this.host = Objects.requireNonNull(host);
            return this;
        }

        public Builder withPort(int port) {
            this.port = port;
            return this;
        }

        public Builder withDatabaseName(@NotNull String name) {
            this.databaseName = Objects.requireNonNull(name);
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

        public Builder withSettingsName(@NotNull String settingsName) {
            this.settingsName = settingsName;
            return this;
        }

        public DatabaseSettings build() {
            AdditionalDatabaseSettings additionalDatabaseSettings = new AdditionalDatabaseSettings(type, postgresSettings);
            return new DatabaseSettings(type, settingsName, host, port, databaseName, login, password, additionalDatabaseSettings);
        }
    }
}
