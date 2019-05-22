package com.blog.ProcessorServiceTests;


import com.blog.ApplicationTests;
import com.blog.TestUtils;
import com.blog.service.processor.BackupCompressor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class BackupCompressorTests extends ApplicationTests {
    private TestUtils testUtils;

    private BackupCompressor backupCompressor;

    @Autowired
    void setTestUtils(TestUtils testUtils) {
        this.testUtils = testUtils;
    }

    @Autowired
    void setBackupCompressor(BackupCompressor backupCompressor) {
        this.backupCompressor = backupCompressor;
    }

    @Test
    void whenCompressAndDecompressBackup_contentIsEqualToSource() throws IOException {
        byte[] source = testUtils.getRandomBytes(1000);

        try (
                InputStream sourceInputStream = new ByteArrayInputStream(source);
                InputStream compressedSourceInputStream = backupCompressor.process(sourceInputStream);
                InputStream decompressedSourceInputStream = backupCompressor.deprocess(compressedSourceInputStream)
        ) {
            assertTrue(testUtils.streamsContentEquals(new ByteArrayInputStream(source), decompressedSourceInputStream));
        }
    }
}
