package com.blog.webUI.formTransfer;

import java.util.ArrayList;
import java.util.HashMap;

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
        private String storageSettingsName;

        private ArrayList<String> processors = new ArrayList<>();

        public String getStorageSettingsName() {
            return storageSettingsName;
        }

        public void setStorageSettingsName(String storageSettingsName) {
            this.storageSettingsName = storageSettingsName;
        }

        public ArrayList<String> getProcessors() {
            return processors;
        }

        public void setProcessors(ArrayList<String> processors) {
            this.processors = processors;
        }

        @Override
        public String toString() {
            return "BackupCreationProperties{" +
                    "storageSettingsName='" + storageSettingsName + '\'' +
                    ", processors=" + processors +
                    '}';
        }
    }
}
