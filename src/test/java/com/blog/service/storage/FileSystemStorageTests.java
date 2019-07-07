package com.blog.service.storage;

import com.blog.ApplicationTests;
import com.blog.entities.storage.StorageSettings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;

import static com.blog.TestUtils.equalToSourceInputStream;
import static com.blog.TestUtils.getRandomBytes;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;

class FileSystemStorageTests extends ApplicationTests {
    private static final Integer testTaskID = 0;

    private FileSystemStorage fileSystemStorage;

    private StorageSettings localFileSystemStorageSettings;

    @Autowired
    void setFileSystemStorage(FileSystemStorage fileSystemStorage) {
        this.fileSystemStorage = fileSystemStorage;
    }

    @Autowired
    void setLocalFileSystemStorageSettings(StorageSettings localFileSystemStorageSettings) {
        this.localFileSystemStorageSettings = localFileSystemStorageSettings;
    }

    @Test
    void whenUploadBackupAndDownload_contentIsEqual(TestInfo testInfo) throws IOException {
        String backupName = testInfo.getDisplayName() + "_" + StorageConstants.dateFormatter.format(LocalDateTime.now());
        byte[] source = getRandomBytes(1000000);

        try (
                ByteArrayInputStream sourceInputStream = new ByteArrayInputStream(source)
        ) {
            fileSystemStorage.uploadBackup(sourceInputStream, localFileSystemStorageSettings, backupName, testTaskID);
            try (
                    InputStream downloadedBackup = fileSystemStorage.downloadBackup(localFileSystemStorageSettings, backupName, testTaskID)
            ) {
                assertThat(downloadedBackup, equalToSourceInputStream(new ByteArrayInputStream(source)));
            }
        }
    }

    @Test
    void whenUploadBackupAndDelete_backupIsDeletedOnStorage(TestInfo testInfo) throws IOException {
        String backupName = testInfo.getDisplayName() + "_" + StorageConstants.dateFormatter.format(LocalDateTime.now());
        byte[] source = getRandomBytes(1000000);

        try (
                ByteArrayInputStream sourceInputStream = new ByteArrayInputStream(source)
        ) {
            fileSystemStorage.uploadBackup(sourceInputStream, localFileSystemStorageSettings, backupName, testTaskID);
            fileSystemStorage.deleteBackup(localFileSystemStorageSettings, backupName, testTaskID);

            assertFalse(new File(localFileSystemStorageSettings.getLocalFileSystemSettings().orElseThrow(RuntimeException::new).
                    getBackupPath() + File.separator + backupName).exists());
        }
    }

    @Test
    void deleteBackup_deletionShouldBeIdempotent(TestInfo testInfo) throws IOException {
        String backupName = testInfo.getDisplayName() + "_" + StorageConstants.dateFormatter.format(LocalDateTime.now());
        byte[] source = getRandomBytes(1000000);

        try (
                ByteArrayInputStream sourceInputStream = new ByteArrayInputStream(source)
        ) {
            fileSystemStorage.uploadBackup(sourceInputStream, localFileSystemStorageSettings, backupName, testTaskID);
            fileSystemStorage.deleteBackup(localFileSystemStorageSettings, backupName, testTaskID);

            assertDoesNotThrow(() -> fileSystemStorage.deleteBackup(localFileSystemStorageSettings, backupName, testTaskID));
        }
    }
}