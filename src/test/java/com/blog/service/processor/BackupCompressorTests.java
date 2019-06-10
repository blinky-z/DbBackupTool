package com.blog.service.processor;


import com.blog.ApplicationTests;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static com.blog.TestUtils.equalToSourceInputStream;
import static com.blog.TestUtils.getRandomBytes;
import static org.hamcrest.MatcherAssert.assertThat;

class BackupCompressorTests extends ApplicationTests {
    private BackupCompressor backupCompressor;

    @Autowired
    void setBackupCompressor(BackupCompressor backupCompressor) {
        this.backupCompressor = backupCompressor;
    }

    @Test
    void whenCompressAndDecompressBackup_contentIsEqualToSource() throws IOException {
        byte[] source = getRandomBytes(1000);

        try (
                InputStream sourceInputStream = new ByteArrayInputStream(source);
                InputStream compressedSourceInputStream = backupCompressor.process(sourceInputStream);
                InputStream decompressedSourceInputStream = backupCompressor.deprocess(compressedSourceInputStream)
        ) {
            assertThat(decompressedSourceInputStream, equalToSourceInputStream(new ByteArrayInputStream(source)));
        }
    }
}
