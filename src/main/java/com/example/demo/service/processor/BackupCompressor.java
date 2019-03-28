package com.example.demo.service.processor;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.Objects;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@Service
public class BackupCompressor {
    private static final class GZipCompressor implements Runnable {
        InputStream backupStream;

        GZIPOutputStream gzipOutputStream;

        GZipCompressor(InputStream backupStream, GZIPOutputStream gzipOutputStream) {
            this.backupStream = backupStream;
            this.gzipOutputStream = gzipOutputStream;
        }

        public void run() {
            try (
                    BufferedInputStream bufferedInputStream = new BufferedInputStream(backupStream);
                    BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(gzipOutputStream)
            ) {
                final byte[] buffer = new byte[8 * 1024];
                int bytesRead;
                while ((bytesRead = bufferedInputStream.read(buffer)) != -1) {
                    bufferedOutputStream.write(buffer, 0, bytesRead);
                }
            } catch (IOException ex) {
                throw new RuntimeException("Error occurred while compressing backup", ex);
            }
        }
    }

    public InputStream compressBackup(@NotNull InputStream backupStream) {
        Objects.requireNonNull(backupStream);
        try {
            PipedOutputStream out = new PipedOutputStream();

            PipedInputStream in = new PipedInputStream();
            in.connect(out);

            GZIPOutputStream gzipOutputStream = new GZIPOutputStream(out);

            Thread backupCompressorThread = new Thread(new GZipCompressor(backupStream, gzipOutputStream));
            backupCompressorThread.start();

            return in;
        } catch (IOException ex) {
            throw new RuntimeException("Error occurred while compressing backup", ex);
        }
    }

    public InputStream decompressBackup(@NotNull InputStream backupStream) {
        Objects.requireNonNull(backupStream);
        try {
            return new GZIPInputStream(backupStream);
        } catch (IOException ex) {
            throw new RuntimeException("Error occurred while decompressing backup", ex);
        }
    }
}
