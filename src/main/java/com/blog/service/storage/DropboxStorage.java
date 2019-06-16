package com.blog.service.storage;

import com.blog.entities.storage.DropboxSettings;
import com.blog.entities.storage.StorageSettings;
import com.blog.service.ErrorCallbackService;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

/**
 * Implementation of the {@link Storage} interface for Dropbox.
 */
@Service
public class DropboxStorage implements Storage {
    private static final Logger logger = LoggerFactory.getLogger(DropboxStorage.class);

    private ExecutorService dropboxExecutorService;

    private ErrorCallbackService errorCallbackService;

    @Autowired
    public void setDropboxExecutorService(ExecutorService dropboxExecutorService) {
        this.dropboxExecutorService = dropboxExecutorService;
    }

    @Autowired
    public void setErrorCallbackService(ErrorCallbackService errorCallbackService) {
        this.errorCallbackService = errorCallbackService;
    }

    private String getCurrentFilePartAsAbsolutePath(String backupFolderPath, String backupName, int backupPart) {
        String filename = String.format(StorageConstants.DEFAULT_FILENAME_TEMPLATE, backupName, backupPart);
        return backupFolderPath + "/" + filename + StorageConstants.DEFAULT_FILE_EXTENSION;
    }

    private String getBackupFolderPathByBackupName(String backupName) {
        return "/" + backupName;
    }

    /**
     * Uploads backup to Dropbox.
     * <p>
     * Backup is saved into root folder, which is usually an app folder depending on token type.
     */
    @Override
    public void uploadBackup(@NotNull InputStream in, @NotNull StorageSettings storageSettings, @NotNull String backupName,
                             @NotNull Integer id) {
        Objects.requireNonNull(in);
        Objects.requireNonNull(storageSettings);
        Objects.requireNonNull(backupName);
        Objects.requireNonNull(id);

        String backupFolderPath = getBackupFolderPathByBackupName(backupName);
        DbxRequestConfig config = DbxRequestConfig.newBuilder("dbBackupUploader").build();
        DropboxSettings dropboxSettings = storageSettings.getDropboxSettings().orElseThrow(() -> new RuntimeException(
                "Can't upload backup to Dropbox storage: Missing Dropbox Settings"));
        DbxClientV2 dbxClient = new DbxClientV2(config, dropboxSettings.getAccessToken());

        try (BufferedInputStream bufferedInputStream = new BufferedInputStream(in);

             ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(byteArrayOutputStream)
        ) {
            int currentBackupPart = 0;
            long maxChunkSize = 64L * 1024 * 1024;
            int bytesRead;
            long currentChunkSize = 0;

            byte[] buffer = new byte[64 * 1024];
            while ((bytesRead = bufferedInputStream.read(buffer)) != -1) {
                currentChunkSize += bytesRead;
                bufferedOutputStream.write(buffer, 0, bytesRead);

                if (currentChunkSize >= maxChunkSize) {
                    bufferedOutputStream.flush();
                    String currentFilePath = getCurrentFilePartAsAbsolutePath(backupFolderPath, backupName, currentBackupPart);

                    dbxClient.files().uploadBuilder(currentFilePath).uploadAndFinish(
                            new ByteArrayInputStream(byteArrayOutputStream.toByteArray()));

                    currentBackupPart++;
                    byteArrayOutputStream.reset();
                    currentChunkSize = 0;
                }
            }
            if (currentChunkSize != 0) {
                bufferedOutputStream.flush();
                String currentFilePath = getCurrentFilePartAsAbsolutePath(backupFolderPath, backupName, currentBackupPart);

                dbxClient.files().uploadBuilder(currentFilePath).uploadAndFinish(
                        new ByteArrayInputStream(byteArrayOutputStream.toByteArray()));

                byteArrayOutputStream.reset();
            }
        } catch (InterruptedIOException ex) {
            logger.error("Uploading to Dropbox was interrupted. Backup folder: {}", backupFolderPath);
        } catch (DbxException | IOException ex) {
            throw new RuntimeException("Error occurred while uploading backup to Dropbox", ex);
        }
    }

    @Nullable
    @Override
    public InputStream downloadBackup(@NotNull StorageSettings storageSettings, @NotNull String backupName, @NotNull Integer id) {
        Objects.requireNonNull(storageSettings);
        Objects.requireNonNull(backupName);
        Objects.requireNonNull(id);

        String backupFolderPath = "/" + backupName;
        DbxRequestConfig config = DbxRequestConfig.newBuilder("dbBackupDownloader").build();
        DropboxSettings dropboxSettings = storageSettings.getDropboxSettings().orElseThrow(() ->
                new RuntimeException("Can't download backup from Dropbox storage: Missing Dropbox Settings"));
        DbxClientV2 dbxClient = new DbxClientV2(config, dropboxSettings.getAccessToken());

        int filesCount;
        try {
            ListFolderResult listFolderResult = dbxClient.files().listFolder(backupFolderPath);
            List<Metadata> listFolderMetadata = listFolderResult.getEntries();
            filesCount = listFolderMetadata.size();
        } catch (DbxException ex) {
            throw new RuntimeException("Error listing Dropbox backup folder. Backup folder: " + backupFolderPath, ex);
        }

        try {
            PipedOutputStream out = new PipedOutputStream();
            PipedInputStream in = new PipedInputStream();
            out.connect(in);

            dropboxExecutorService.submit(new BackupDownloader(out, dbxClient, backupFolderPath, backupName, filesCount, id));

            return in;
        } catch (IOException ex) {
            throw new RuntimeException("Error initializing backup downloading from Dropbox", ex);
        }
    }

    @Override
    public void deleteBackup(@NotNull StorageSettings storageSettings, @NotNull String backupName, @NotNull Integer id) {
        Objects.requireNonNull(storageSettings);
        Objects.requireNonNull(backupName);
        Objects.requireNonNull(id);

        String backupFolderPath = getBackupFolderPathByBackupName(backupName);
        DbxRequestConfig config = DbxRequestConfig.newBuilder("dbBackupDeleted").build();
        DropboxSettings dropboxSettings = storageSettings.getDropboxSettings().orElseThrow(() -> new RuntimeException(
                "Can't delete backup from Dropbox storage: Missing Dropbox Settings"));
        DbxClientV2 dbxClient = new DbxClientV2(config, dropboxSettings.getAccessToken());

        try {
            dbxClient.files().deleteV2(backupFolderPath);
        } catch (DbxException ex) {
            throw new RuntimeException("Error deleting backup from Dropbox. Backup folder: " + backupFolderPath, ex);
        }
    }

    private class BackupDownloader implements Runnable {
        private OutputStream out;

        private DbxClientV2 dbxClient;

        private int filesCount;

        private String backupFolderPath;

        private String backupName;

        private Integer id;

        BackupDownloader(PipedOutputStream out, DbxClientV2 dbxClient, String backupFolderPath,
                         String backupName, int filesCount, Integer id) {
            this.out = out;
            this.dbxClient = dbxClient;
            this.backupFolderPath = backupFolderPath;
            this.backupName = backupName;
            this.filesCount = filesCount;
            this.id = id;
        }

        public void run() {
            try {
                logger.info("Total files in backup folder on Dropbox: {}. Backup folder: {}", filesCount, backupFolderPath);
                String currentFile;
                for (int currentBackupPart = 0; currentBackupPart < filesCount; currentBackupPart++) {
                    currentFile = getCurrentFilePartAsAbsolutePath(backupFolderPath, backupName, currentBackupPart);
                    dbxClient.files().downloadBuilder(currentFile).download(out);
                }
            } catch (DbxException | IOException ex) {
                // if stream is closed, that means work was interrupted, so it is not an error
                if (!ex.getMessage().equals("Pipe closed")) {
                    errorCallbackService.onError(new RuntimeException("Error occurred while downloading backup from Dropbox. Backup folder: " +
                            backupFolderPath, ex), id);
                }
            } finally {
                try {
                    out.close();
                } catch (IOException ex) {
                    logger.error("Dropbox backup downloader: error closing output stream. Backup folder: {}", backupFolderPath, ex);
                }
            }
        }
    }
}
