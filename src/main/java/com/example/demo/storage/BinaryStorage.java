package com.example.demo.storage;

import java.io.InputStream;

/**
 * This interface provides API to handle binary backups, i.e. archive backups.
 * If you have plain text backup, use TextStorage interface
 */
public interface BinaryStorage extends Storage {
    void saveBackup(byte[] data);

    InputStream downloadBackup();
}
