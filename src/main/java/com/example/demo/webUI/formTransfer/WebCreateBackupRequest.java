package com.example.demo.webUI.formTransfer;

import java.util.List;

public class WebCreateBackupRequest {
    private List<Integer> checkStorageList;

    private List<Integer> checkDatabaseList;

    int maxChunkSize;

    public List<Integer> getCheckStorageList() {
        return checkStorageList;
    }

    public void setCheckStorageList(List<Integer> checkStorageList) {
        this.checkStorageList = checkStorageList;
    }

    public List<Integer> getCheckDatabaseList() {
        return checkDatabaseList;
    }

    public void setCheckDatabaseList(List<Integer> checkDatabaseList) {
        this.checkDatabaseList = checkDatabaseList;
    }

    public int getMaxChunkSize() {
        return maxChunkSize;
    }

    public void setMaxChunkSize(int maxChunkSize) {
        this.maxChunkSize = maxChunkSize;
    }
}
