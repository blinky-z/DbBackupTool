package com.example.demo.StorageHandler;

import java.io.InputStream;

public interface BinaryStorageHandler extends StorageHandler {
    public void saveBackup(byte[] data);

    public InputStream downloadBackup();
}
