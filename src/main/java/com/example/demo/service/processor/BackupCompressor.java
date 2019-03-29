package com.example.demo.service.processor;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.Objects;
import java.util.zip.*;

@Service
public class BackupCompressor {
    public InputStream compressBackup(@NotNull InputStream uncompressedBackup) {
        Objects.requireNonNull(uncompressedBackup);
        return new DeflaterInputStream(uncompressedBackup);
    }

    public InputStream decompressBackup(@NotNull InputStream compressedBackup) {
        Objects.requireNonNull(compressedBackup);
        return new InflaterInputStream(compressedBackup);
    }
}
