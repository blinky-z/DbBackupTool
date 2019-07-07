package com.blog.service.storage;

import com.blog.ApplicationTests;
import com.blog.entities.storage.DropboxSettings;
import com.blog.entities.storage.StorageSettings;
import com.blog.manager.StorageSettingsManager;
import com.dropbox.core.BadRequestException;
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
import java.time.ZoneOffset;

import static com.blog.TestUtils.equalToSourceInputStream;
import static com.blog.TestUtils.getRandomBytes;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class DropboxStorageTests extends ApplicationTests {
    private static final Integer testTaskID = 0;

    private DropboxStorage dropboxStorage;

    private StorageSettings dropboxStorageSettings;

    private StorageSettingsManager storageSettingsManager;

    @Autowired
    void setDropboxStorageSettings(StorageSettings dropboxStorageSettings) {
        this.dropboxStorageSettings = dropboxStorageSettings;
    }

    @Autowired
    void setDropboxStorage(DropboxStorage dropboxStorage) {
        this.dropboxStorage = dropboxStorage;
    }

    @Autowired
    public void setStorageSettingsManager(StorageSettingsManager storageSettingsManager) {
        this.storageSettingsManager = storageSettingsManager;
    }

    @Test
    void whenUploadBackupAndDownload_contentIsEqual(TestInfo testInfo) throws IOException {
        String backupName = testInfo.getDisplayName() + "_" + StorageConstants.dateFormatter.format(LocalDateTime.now());
        byte[] source = getRandomBytes(1000000);

        try (
                ByteArrayInputStream sourceInputStream = new ByteArrayInputStream(source)
        ) {
            dropboxStorage.uploadBackup(sourceInputStream, dropboxStorageSettings, backupName, testTaskID);
            try (
                    InputStream downloadedBackup = dropboxStorage.downloadBackup(dropboxStorageSettings, backupName, testTaskID)
            ) {
                assertThat(downloadedBackup, equalToSourceInputStream(new ByteArrayInputStream(source)));
            }
        }
    }

    @Test
    void whenUploadBackupAndDelete_delete_shouldDeleteBackupFromStorage(TestInfo testInfo) throws IOException, DbxException {
        String backupName = testInfo.getDisplayName() + "_" + StorageConstants.dateFormatter.format(LocalDateTime.now());
        byte[] source = getRandomBytes(1000000);

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

    @Test
    void whenPassMalformedAuthToken_deleteBackup_shouldThrowAnException(TestInfo testInfo) {
        DropboxSettings dropboxSettings = new DropboxSettings();
        dropboxSettings.setAccessToken("MalformedAuthToken0");

        StorageSettings dropboxStorageSettings = storageSettingsManager.save(
                StorageSettings.dropboxSettings(dropboxSettings)
                        .withSettingsName(testInfo.getDisplayName())
                        .withDate(LocalDateTime.now(ZoneOffset.UTC))
                        .build());

        assertThrows(BadRequestException.class, () -> {
            try {
                dropboxStorage.deleteBackup(dropboxStorageSettings, testInfo.getDisplayName(), testTaskID);
            } catch (Exception ex) {
                throw ex.getCause();
            }
        });
    }

    @Test
    void deleteBackup_deletionShouldBeIdempotent(TestInfo testInfo) throws IOException {
        String backupName = testInfo.getDisplayName() + "_" + StorageConstants.dateFormatter.format(LocalDateTime.now());
        byte[] source = getRandomBytes(1000000);

        try (
                ByteArrayInputStream sourceInputStream = new ByteArrayInputStream(source)
        ) {
            dropboxStorage.uploadBackup(sourceInputStream, dropboxStorageSettings, backupName, testTaskID);
            dropboxStorage.deleteBackup(dropboxStorageSettings, backupName, testTaskID);

            assertDoesNotThrow(() -> dropboxStorage.deleteBackup(dropboxStorageSettings, backupName, testTaskID));
        }
    }
}
