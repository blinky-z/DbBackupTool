package com.example.demo.models.storage;

public class LocalFileSystemSettings {
    private String backupPath;

    public String getBackupPath() {
        return backupPath;
    }

    public void setBackupPath(String backupPath) {
        this.backupPath = backupPath;
    }

    @Override
    public String toString() {
        return "LocalFileSystemSettings{" +
                ", backupPath='" + backupPath + '\'' +
                '}';
    }
}
