package com.example.demo.Storage;

import java.io.InputStream;

/**
 * This interface provides API to handle plain text backups.
 */
public interface TextStorage extends Storage {
    void saveBackup(String data);

    InputStream downloadBackup();
}
