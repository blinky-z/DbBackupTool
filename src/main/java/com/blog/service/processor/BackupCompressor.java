package com.blog.service.processor;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger logger = LoggerFactory.getLogger(BackupCompressor.class);

    private static final String UNCOMPRESSED_BACKUP_STREAM_MUST_NOT_BE_NULL = "Uncompressed backup stream must not be null!";

    private static final String COMPRESSED_BACKUP_STREAM_MUST_NOT_BE_NULL = "Compressed backup stream must not be null!";

    private static final String processorName = "Compressor";

    /**
     * Compresses backup.
     *
     * @param uncompressedBackup the stream contains data to compress
     * @return input stream, from which compressed data can be read
     */
    public InputStream process(@NotNull InputStream uncompressedBackup) {
        logger.info("Compressing backup");
        Objects.requireNonNull(uncompressedBackup, UNCOMPRESSED_BACKUP_STREAM_MUST_NOT_BE_NULL);
        return new DeflaterInputStream(uncompressedBackup);
    }

    /**
     * Decompresses backup.
     *
     * @param compressedBackup the stream contains compressed data
     * @return input stream, from which decompressed data can be read
     */
    public InputStream deprocess(@NotNull InputStream compressedBackup) {
        logger.info("Decompressing backup");
        Objects.requireNonNull(compressedBackup, COMPRESSED_BACKUP_STREAM_MUST_NOT_BE_NULL);
        return new InflaterInputStream(compressedBackup);
    }

    @Override
    public String getName() {
        return processorName;
    }
}
