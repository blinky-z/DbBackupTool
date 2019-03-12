package com.example.demo.StorageHandler;

import java.io.InputStream;

/**
 * This interface provides API to handle binary backups, i.e. archive backups.
 * If you have plain text backup, use TextStorage interface
 */
public interface BinaryStorageHandler extends StorageHandler {
    void saveBackup(byte[] data);

    InputStream downloadBackup();
}
