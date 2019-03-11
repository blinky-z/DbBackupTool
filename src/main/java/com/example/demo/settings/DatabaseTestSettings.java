package com.example.demo.settings;

import javax.sql.DataSource;
import java.sql.SQLException;

public class DatabaseTestSettings implements DatabaseSettings {
    private String connectionUrl;

    private String databaseUser;

    private String databasePassword;

    private String databaseName;

    public DatabaseTestSettings(DataSource dataSource) {
        try {
            this.connectionUrl = dataSource.getConnection().getMetaData().getURL();
            this.databaseUser = "postgres";
            this.databasePassword = "postgres";
            this.databaseName = this.connectionUrl.substring(this.connectionUrl.lastIndexOf("/") + 1);
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

    @Override
    public String getDatabaseName() {
        return databaseName;
    }
}
