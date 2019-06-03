package com.blog.service.storage;

import com.blog.ApplicationTests;
import com.blog.TestUtils;
import com.blog.entities.storage.DropboxSettings;
import com.blog.entities.storage.StorageSettings;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.GetMetadataErrorException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void whenUploadSmallBackupAndDownload_contentIsEqual(TestInfo testInfo) throws IOException {
        String backupName = testInfo.getDisplayName() + "_" + StorageConstants.dateFormatter.format(LocalDateTime.now());
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
    void whenUploadBigBackupAndDownload_contentIsEqual(TestInfo testInfo) throws IOException {
        String backupName = testInfo.getDisplayName() + "_" + StorageConstants.dateFormatter.format(LocalDateTime.now());
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
    void whenUploadBackupAndDelete_backupIsDeletedOnStorage(TestInfo testInfo) throws IOException, DbxException {
        String backupName = testInfo.getDisplayName() + "_" + StorageConstants.dateFormatter.format(LocalDateTime.now());
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
