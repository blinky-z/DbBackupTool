package com.example.demo.WebUiSettings;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

public class CreateBackupSettings {
    @Valid
    @NotEmpty
    private String databaseType;

    @Valid
    private DatabaseSettings databaseSettings;

    public String getDatabaseType() {
        return databaseType;
    }

    public void setDatabaseType(String databaseType) {
        this.databaseType = databaseType;
    }

    public DatabaseSettings getDatabaseSettings() {
        return databaseSettings;
    }

    public void setDatabaseSettings(DatabaseSettings databaseSettings) {
        this.databaseSettings = databaseSettings;
    }

}
