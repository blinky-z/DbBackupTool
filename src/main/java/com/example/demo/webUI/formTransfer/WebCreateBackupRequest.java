package com.example.demo.webUI.formTransfer;

import java.util.ArrayList;
import java.util.HashMap;

public class WebCreateBackupRequest {
    public static final class BackupCreationProperties {
        private Integer id;

        private ArrayList<String> processors = new ArrayList<>();

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public ArrayList<String> getProcessors() {
            return processors;
        }

        public void setProcessors(ArrayList<String> processors) {
            this.processors = processors;
        }
    }

    private Integer databaseId;

    private HashMap<Integer, BackupCreationProperties> backupCreationProperties = new HashMap<>();

    public HashMap<Integer, BackupCreationProperties> getBackupCreationProperties() {
        return backupCreationProperties;
    }

    public void setBackupCreationProperties(HashMap<Integer, BackupCreationProperties> backupCreationProperties) {
        this.backupCreationProperties = backupCreationProperties;
    }

    public Integer getDatabaseId() {
        return databaseId;
    }

    public void setDatabaseId(Integer databaseId) {
        this.databaseId = databaseId;
    }
}
