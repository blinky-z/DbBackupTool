package com.example.demo.service.storage;

import java.io.InputStream;

/**
 * General Storage interface
 */
public interface Storage {
    /**
     * Saves backup on specified storage
     *
     * @param in the input stream to read backup from
     */
    public void uploadBackup(InputStream in);

    /**
     * Downloads backup from the specified storage
     *
     * @return input stream, from which backup can be read
     */
    public InputStream downloadBackup();
}
