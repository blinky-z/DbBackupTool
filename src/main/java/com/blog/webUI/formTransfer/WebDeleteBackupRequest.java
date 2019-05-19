package com.blog.webUI.formTransfer;


/**
 * This class represents deleting created backup form
 * <p>
 * Passed to router "/delete-backup" on DELETE request
 */
public class WebDeleteBackupRequest {
    private String backupId;

    public String getBackupId() {
        return backupId;
    }

    public void setBackupId(String backupId) {
        this.backupId = backupId;
    }

    @Override
    public String toString() {
        return "WebDeleteBackupRequest{" +
                "backupId='" + backupId + '\'' +
                '}';
    }
}
