package com.example.demo.webUI.formTransfer;

import java.util.List;

public class WebCreateBackupRequest {
    private int databaseId;

    private List<Integer> checkStorageList;

    private int maxChunkSize;

    private boolean compress;

    public int getDatabaseId() {
        return databaseId;
    }

    public void setDatabaseId(int databaseId) {
        this.databaseId = databaseId;
    }

    public List<Integer> getCheckStorageList() {
        return checkStorageList;
    }

    public void setCheckStorageList(List<Integer> checkStorageList) {
        this.checkStorageList = checkStorageList;
    }

    public int getMaxChunkSize() {
        return maxChunkSize;
    }

    public void setMaxChunkSize(int maxChunkSize) {
        this.maxChunkSize = maxChunkSize;
    }

    public boolean isCompress() {
        return compress;
    }

    public void setCompress(boolean compress) {
        this.compress = compress;
    }
}
