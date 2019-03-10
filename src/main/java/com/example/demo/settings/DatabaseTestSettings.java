package com.example.demo.settings;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.SQLException;

@Profile("test")
@Qualifier(value = "db-settings")
@Component
public class DatabaseTestSettings implements DatabaseSettings {
    private String connectionUrl;

    private String databaseUser;

    private String databasePassword;

    @Autowired
    DatabaseTestSettings(DataSource dataSource) {
        try {
            this.connectionUrl = dataSource.getConnection().getMetaData().getURL();
            this.databaseUser = "postgres";
            this.databasePassword = "postgres";
        } catch (SQLException ex) {
            throw new RuntimeException("Error construction database test settings", ex);
        }
    }

    @Override
    public String getUrl() {
        return connectionUrl;
    }

    @Override
    public String getUsername() {
        return databaseUser;
    }

    @Override
    public String getPassword() {
        return databasePassword;
    }
}
