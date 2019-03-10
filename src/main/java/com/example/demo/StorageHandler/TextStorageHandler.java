package com.example.demo.StorageHandler;

import java.io.InputStream;

public interface TextStorageHandler extends StorageHandler {
    public void saveBackup(String data);

    public InputStream downloadBackup();
}
