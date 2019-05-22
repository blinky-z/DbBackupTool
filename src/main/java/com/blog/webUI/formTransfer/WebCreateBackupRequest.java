package com.blog.webUI.formTransfer;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * This class represents creating database backup form
 * <p>
 * Passed to router "/create-backup" on POST request
 */
public class WebCreateBackupRequest {
    private String databaseSettingsName;

    private HashMap<String, BackupCreationProperties> backupCreationProperties = new HashMap<>();

    public HashMap<String, BackupCreationProperties> getBackupCreationProperties() {
        return backupCreationProperties;
    }

    public void setBackupCreationProperties(HashMap<String, BackupCreationProperties> backupCreationProperties) {
        this.backupCreationProperties = backupCreationProperties;
    }

    public String getDatabaseSettingsName() {
        return databaseSettingsName;
    }

    public void setDatabaseSettingsName(String databaseSettingsName) {
        this.databaseSettingsName = databaseSettingsName;
    }

    @Override
    public String toString() {
        return "WebCreateBackupRequest{" +
                "databaseSettingsName='" + databaseSettingsName + '\'' +
                ", backupCreationProperties=" + backupCreationProperties +
                '}';
    }

    public static final class BackupCreationProperties {
        private ArrayList<String> processors = new ArrayList<>();

        public ArrayList<String> getProcessors() {
            return processors;
        }

        public void setProcessors(ArrayList<String> processors) {
            this.processors = processors;
        }

        @Override
        public String toString() {
            return "BackupCreationProperties{" +
                    ", processors=" + processors +
                    '}';
        }
    }
}
