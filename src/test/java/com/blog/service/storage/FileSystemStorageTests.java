package com.blog.service.storage;

import com.blog.ApplicationTests;
import com.blog.TestUtils;
import com.blog.entities.storage.StorageSettings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class FileSystemStorageTests extends ApplicationTests {
    private static final Integer testTaskID = 0;

    private TestUtils testUtils;

    private FileSystemStorage fileSystemStorage;

    private StorageSettings localFileSystemStorageSettings;

    @Autowired
    void setTestUtils(TestUtils testUtils) {
        this.testUtils = testUtils;
    }

    @Autowired
    void setFileSystemStorage(FileSystemStorage fileSystemStorage) {
        this.fileSystemStorage = fileSystemStorage;
    }

    @Autowired
    void setLocalFileSystemStorageSettings(StorageSettings localFileSystemStorageSettings) {
        this.localFileSystemStorageSettings = localFileSystemStorageSettings;
    }

    @Test
    void whenUploadSmallBackupAndDownload_contentIsEqual(TestInfo testInfo) throws IOException {
        String backupName = testInfo.getDisplayName() + "_" + StorageConstants.dateFormatter.format(LocalDateTime.now());
        byte[] source = testUtils.getRandomBytes(1000);

        try (
                ByteArrayInputStream sourceInputStream = new ByteArrayInputStream(source)
        ) {
            fileSystemStorage.uploadBackup(sourceInputStream, localFileSystemStorageSettings, backupName, testTaskID);
            try (
                    InputStream downloadedBackup =
                            fileSystemStorage.downloadBackup(localFileSystemStorageSettings, backupName, testTaskID)
            ) {
                assertTrue(testUtils.streamsContentEquals(new ByteArrayInputStream(source), downloadedBackup));
            }
        }
    }

    @Test
    void whenUploadBigBackupAndDownload_contentIsEqual(TestInfo testInfo) throws IOException {
        String backupName = testInfo.getDisplayName() + "_" + StorageConstants.dateFormatter.format(LocalDateTime.now());
        byte[] source = testUtils.getRandomBytes(1000000);

        try (
                ByteArrayInputStream sourceInputStream = new ByteArrayInputStream(source)
        ) {
            fileSystemStorage.uploadBackup(sourceInputStream, localFileSystemStorageSettings, backupName, testTaskID);
            try (
                    InputStream downloadedBackup =
                            fileSystemStorage.downloadBackup(localFileSystemStorageSettings, backupName, testTaskID)
            ) {
                assertTrue(testUtils.streamsContentEquals(new ByteArrayInputStream(source), downloadedBackup));
            }
        }
    }

    @Test
    void whenUploadBackupAndDelete_backupIsDeletedOnStorage(TestInfo testInfo) throws IOException {
        String backupName = testInfo.getDisplayName() + "_" + StorageConstants.dateFormatter.format(LocalDateTime.now());
        byte[] source = testUtils.getRandomBytes(1000000);

        try (
                ByteArrayInputStream sourceInputStream = new ByteArrayInputStream(source)
        ) {
            fileSystemStorage.uploadBackup(sourceInputStream, localFileSystemStorageSettings, backupName, testTaskID);
            fileSystemStorage.deleteBackup(localFileSystemStorageSettings, backupName, testTaskID);

            assertFalse(new File(localFileSystemStorageSettings.getLocalFileSystemSettings()
                    .orElseThrow(RuntimeException::new).
                            getBackupPath() + File.separator + backupName).exists());
        }
    }
}