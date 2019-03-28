package com.example.demo.service.storage;

import java.io.InputStream;

/**
 * This interface provides API to handle binary backups, i.e. archive backups.
 * If you have plain text backup, use TextStorage interface
 */
public interface BinaryStorage extends Storage {
    void uploadBackup(byte[] data);

    InputStream downloadBackup();
}
