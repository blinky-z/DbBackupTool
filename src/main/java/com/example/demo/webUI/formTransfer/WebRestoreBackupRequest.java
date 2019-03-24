package com.example.demo.webUI.formTransfer;

import java.util.List;

public class WebRestoreBackupRequest {
    private List<Integer> checkBackupList;

    private List<Integer> checkDatabaseList;

    public List<Integer> getCheckBackupList() {
        return checkBackupList;
    }

    public void setCheckBackupList(List<Integer> checkBackupList) {
        this.checkBackupList = checkBackupList;
    }

    public List<Integer> getCheckDatabaseList() {
        return checkDatabaseList;
    }

    public void setCheckDatabaseList(List<Integer> checkDatabaseList) {
        this.checkDatabaseList = checkDatabaseList;
    }
}
