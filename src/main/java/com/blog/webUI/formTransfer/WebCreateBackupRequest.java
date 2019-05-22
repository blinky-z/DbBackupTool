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

    private HashMap<String, BackupCreationProperties> backupCreationPropertiesMap = new HashMap<>();

    public String getDatabaseSettingsName() {
        return databaseSettingsName;
    }

    public void setDatabaseSettingsName(String databaseSettingsName) {
        this.databaseSettingsName = databaseSettingsName;
    }

    public HashMap<String, BackupCreationProperties> getBackupCreationPropertiesMap() {
        return backupCreationPropertiesMap;
    }

    public void setBackupCreationPropertiesMap(HashMap<String, BackupCreationProperties> backupCreationPropertiesMap) {
        this.backupCreationPropertiesMap = backupCreationPropertiesMap;
    }

    @Override
    public String toString() {
        return "WebCreateBackupRequest{" +
                "databaseSettingsName='" + databaseSettingsName + '\'' +
                ", backupCreationPropertiesMap=" + backupCreationPropertiesMap +
                '}';
    }

    public static final class BackupCreationProperties {
        /**
         * @implNote We need this field just for to be able adding value to map entry from thymeleaf form in case of no other fields in
         * this inner class was selected.
         */
        private boolean selected;

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
