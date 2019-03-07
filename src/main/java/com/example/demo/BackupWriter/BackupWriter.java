package com.example.demo.BackupWriter;

import java.io.IOException;

public abstract class BackupWriter {
    String databaseName;
    // maxChunkSize - max file size in bytes. If 0, then write to single file
    long maxChunkSize;

    BackupWriter(String databaseName, long maxFileSizeInBytes) {
        this.databaseName = databaseName;
        this.maxChunkSize = maxFileSizeInBytes;
    }

    public void write(String data) throws IOException {
        throw new IOException();
    }

    public void write(byte[] data) throws IOException {
        throw new IOException();
    }

    public void close() throws IOException {
        throw new IOException();
    }
}
