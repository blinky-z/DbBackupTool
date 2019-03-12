package com.example.demo.StorageHandler;

import java.io.InputStream;

/**
 * This interface provides API to handle plain text backups.
 */
public interface TextStorageHandler extends StorageHandler {
    void saveBackup(String data);

    InputStream downloadBackup();
}
