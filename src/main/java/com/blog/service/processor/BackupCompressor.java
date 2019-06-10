package com.blog.service.processor;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Objects;
import java.util.zip.DeflaterInputStream;
import java.util.zip.InflaterInputStream;

/**
 * Backup compressor processor.
 */
@Service
public class BackupCompressor implements Processor {
    /**
     * Compresses backup.
     *
     * @param uncompressedBackup the stream contains data to compress
     * @return input stream, from which compressed data can be read
     */
    public InputStream process(@NotNull InputStream uncompressedBackup) {
        Objects.requireNonNull(uncompressedBackup, "Uncompressed backup stream must not be null");
        return new DeflaterInputStream(uncompressedBackup);
    }

    /**
     * Decompresses backup.
     *
     * @param compressedBackup the stream contains compressed data
     * @return input stream, from which decompressed data can be read
     */
    public InputStream deprocess(@NotNull InputStream compressedBackup) {
        Objects.requireNonNull(compressedBackup, "Compressed backup stream must not be null");
        return new InflaterInputStream(compressedBackup);
    }

    @Override
    public ProcessorType getType() {
        return ProcessorType.COMPRESSOR;
    }
}
