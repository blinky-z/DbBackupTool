package com.blog.StorageServiceTests;

import com.blog.ApplicationTests;
import com.blog.TestUtils;
import com.blog.entities.storage.DropboxSettings;
import com.blog.entities.storage.StorageSettings;
import com.blog.service.storage.DropboxStorage;
import com.blog.service.storage.StorageConstants;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.GetMetadataErrorException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class DropboxStorageTests extends ApplicationTests {
    private static final Integer testTaskID = 0;

    private TestUtils testUtils;

    private DropboxStorage dropboxStorage;

    private StorageSettings dropboxStorageSettings;

    @Autowired
    void setTestUtils(TestUtils testUtils) {
        this.testUtils = testUtils;
    }

    @Autowired
    void setDropboxStorageSettings(StorageSettings dropboxStorageSettings) {
        this.dropboxStorageSettings = dropboxStorageSettings;
    }

    @Autowired
    void setDropboxStorage(DropboxStorage dropboxStorage) {
        this.dropboxStorage = dropboxStorage;
    }

    @Test
    void whenUploadSmallBackupAndDownload_contentIsEqual() throws IOException {
        String backupName = "dropboxStorage_whenUploadSmallBackupAndDownload_contentIsEqual";
        backupName = backupName + "_" + StorageConstants.dateFormatter.format(new Date());
        byte[] source = testUtils.getRandomBytes(1000);

        try (
                ByteArrayInputStream sourceInputStream = new ByteArrayInputStream(source)
        ) {
            dropboxStorage.uploadBackup(sourceInputStream, dropboxStorageSettings, backupName, testTaskID);
            try (
                    InputStream downloadedBackup = dropboxStorage.downloadBackup(dropboxStorageSettings, backupName, testTaskID)
            ) {
                assertTrue(testUtils.streamsContentEquals(new ByteArrayInputStream(source), downloadedBackup));
            }
        }
    }

    @Test
    void whenUploadBigBackupAndDownload_contentIsEqual() throws IOException {
        String backupName = "dropboxStorage_whenUploadBigBackupAndDownload_contentIsEqual";
        backupName = backupName + "_" + StorageConstants.dateFormatter.format(new Date());
        byte[] source = testUtils.getRandomBytes(1000000);

        try (
                ByteArrayInputStream sourceInputStream = new ByteArrayInputStream(source)
        ) {
            dropboxStorage.uploadBackup(sourceInputStream, dropboxStorageSettings, backupName, testTaskID);
            try (
                    InputStream downloadedBackup = dropboxStorage.downloadBackup(dropboxStorageSettings, backupName, testTaskID)
            ) {
                assertTrue(testUtils.streamsContentEquals(new ByteArrayInputStream(source), downloadedBackup));
            }
        }
    }

    @Test
    void whenUploadBackupAndDelete_backupIsDeletedOnStorage() throws IOException, DbxException {
        String backupName = "whenUploadBackupAndDelete_backupIsDeletedOnStorage";
        backupName = backupName + "_" + StorageConstants.dateFormatter.format(new Date());
        byte[] source = testUtils.getRandomBytes(1000000);

        try (
                ByteArrayInputStream sourceInputStream = new ByteArrayInputStream(source)
        ) {
            dropboxStorage.uploadBackup(sourceInputStream, dropboxStorageSettings, backupName, testTaskID);
            dropboxStorage.deleteBackup(dropboxStorageSettings, backupName, testTaskID);

            DbxRequestConfig config = DbxRequestConfig.newBuilder("dbBackupDeleted").build();
            DropboxSettings dropboxSettings = dropboxStorageSettings.getDropboxSettings().orElseThrow(RuntimeException::new);
            DbxClientV2 dbxClient = new DbxClientV2(config, dropboxSettings.getAccessToken());

            try {
                dbxClient.files().getMetadata("/" + backupName);
            } catch (GetMetadataErrorException ex) {
                if (ex.errorValue.isPath()) {
                    assertTrue(ex.errorValue.getPathValue().isNotFound());
                } else {
                    throw ex;
                }
            }
        }
    }
}
