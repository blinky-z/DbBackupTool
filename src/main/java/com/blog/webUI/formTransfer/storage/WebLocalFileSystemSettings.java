package com.blog.webUI.formTransfer.storage;

public class WebLocalFileSystemSettings {
    private String backupPath;

    public String getBackupPath() {
        return backupPath;
    }

    public void setBackupPath(String backupPath) {
        this.backupPath = backupPath;
    }

    @Override
    public String toString() {
        return "WebLocalFileSystemSettings{" +
                "backupPath='" + backupPath + '\'' +
                '}';
    }
}
