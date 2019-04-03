package com.example.demo.webUI.formTransfer;

import java.util.ArrayList;

public class WebCreateBackupRequest {
    public static final class StorageProperties {
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

    private ArrayList<StorageProperties> storageProperties = new ArrayList<>();

    public ArrayList<StorageProperties> getStorageProperties() {
        return storageProperties;
    }

    public void setStorageProperties(ArrayList<StorageProperties> storageProperties) {
        this.storageProperties = storageProperties;
    }

    public int getDatabaseId() {
        return databaseId;
    }

    public void setDatabaseId(int databaseId) {
        this.databaseId = databaseId;
    }
}
