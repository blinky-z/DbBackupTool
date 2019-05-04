package com.blog.StorageServiceTests;

import com.blog.ApplicationTests;
import com.blog.TestUtils;
import com.blog.entities.storage.StorageSettings;
import com.blog.service.storage.FileSystemStorage;
import com.blog.service.storage.Storage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest
public class FileSystemStorageTests extends ApplicationTests {
    private TestUtils testUtils;

    private FileSystemStorage fileSystemStorage;

    private StorageSettings localFileSystemStorageSettings;

    @Autowired
    public void setTestUtils(TestUtils testUtils) {
        this.testUtils = testUtils;
    }

    @Autowired
    public void setFileSystemStorage(FileSystemStorage fileSystemStorage) {
        this.fileSystemStorage = fileSystemStorage;
    }

    @Autowired
    public void setLocalFileSystemStorageSettings(StorageSettings localFileSystemStorageSettings) {
        this.localFileSystemStorageSettings = localFileSystemStorageSettings;
    }

    @Test
    public void whenUploadSmallBackupAndDownload_contentIsEqual() throws IOException {
        String backupName = "fileSystemStorage_whenUploadSmallBackupAndDownload_contentIsEqual";
        backupName = backupName + "_" + Storage.dateFormatter.format(new Date());
        byte[] source = testUtils.getRandomBytes(1000);

        try (
                ByteArrayInputStream sourceInputStream = new ByteArrayInputStream(source)
        ) {
            fileSystemStorage.uploadBackup(sourceInputStream, localFileSystemStorageSettings, backupName);
            try (
                    InputStream downloadedBackup = fileSystemStorage.downloadBackup(localFileSystemStorageSettings, backupName)
            ) {
                assertTrue(testUtils.streamsContentEquals(new ByteArrayInputStream(source), downloadedBackup));
            }
        }
    }

    @Test
    public void whenUploadBigBackupAndDownload_contentIsEqual() throws IOException {
        String backupName = "fileSystemStorage_whenUploadBigBackupAndDownload_contentIsEqual";
        backupName = backupName + "_" + Storage.dateFormatter.format(new Date());
        byte[] source = testUtils.getRandomBytes(1000000);

        try (
                ByteArrayInputStream sourceInputStream = new ByteArrayInputStream(source)
        ) {
            fileSystemStorage.uploadBackup(sourceInputStream, localFileSystemStorageSettings, backupName);
            try (
                    InputStream downloadedBackup = fileSystemStorage.downloadBackup(localFileSystemStorageSettings, backupName)
            ) {
                assertTrue(testUtils.streamsContentEquals(new ByteArrayInputStream(source), downloadedBackup));
            }
        }
    }
}