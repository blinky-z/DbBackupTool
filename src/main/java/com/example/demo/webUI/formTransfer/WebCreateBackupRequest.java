package com.example.demo.webUI.formTransfer;

import java.util.ArrayList;

public class WebCreateBackupRequest {
    public static final class BackupCreationProperties {
        private int id;

        private ArrayList<String> processors = new ArrayList<>();

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public ArrayList<String> getProcessors() {
            return processors;
        }

        public void setProcessors(ArrayList<String> processors) {
            this.processors = processors;
        }
    }

    private int databaseId;

    private ArrayList<BackupCreationProperties> backupCreationProperties = new ArrayList<>();

    public ArrayList<BackupCreationProperties> getBackupCreationProperties() {
        return backupCreationProperties;
    }

    public void setBackupCreationProperties(ArrayList<BackupCreationProperties> backupCreationProperties) {
        this.backupCreationProperties = backupCreationProperties;
    }

    public int getDatabaseId() {
        return databaseId;
    }

    public void setDatabaseId(int databaseId) {
        this.databaseId = databaseId;
    }
}
