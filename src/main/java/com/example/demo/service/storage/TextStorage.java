package com.example.demo.service.storage;

import java.io.InputStream;

/**
 * This interface provides API to handle plain text backups.
 */
public interface TextStorage extends Storage {
    void uploadBackup(String data);

    InputStream downloadBackup();
}
