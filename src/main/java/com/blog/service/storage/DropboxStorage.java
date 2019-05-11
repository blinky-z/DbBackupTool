package com.blog.service.storage;

import com.blog.entities.storage.DropboxSettings;
import com.blog.entities.storage.StorageSettings;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.List;

@Service
public class DropboxStorage implements Storage {
    private static final Logger logger = LoggerFactory.getLogger(DropboxStorage.class);

    private String getCurrentFilePartAsAbsolutePath(String backupFolderPath, String backupName, long backupPart) {
        String filename = String.format(FILENAME_TEMPLATE, backupName, backupPart);
        return backupFolderPath + "/" + filename + FILE_EXTENSION;
    }

    /**
     * Uploads backup to Dropbox
     * <p>
     * Backup is saved into root folder, which is usually an app folder
     */
    public void uploadBackup(@NotNull InputStream in, @NotNull StorageSettings storageSettings, @NotNull String backupName) {
        String backupFolderPath = "/" + backupName;
        DbxRequestConfig config = DbxRequestConfig.newBuilder("dbBackupUploader").build();
        DropboxSettings dropboxSettings = storageSettings.getDropboxSettings().orElseThrow(() -> new RuntimeException(
                "Can't upload backup to Dropbox storage: Missing Dropbox Settings"));
        DbxClientV2 dbxClient = new DbxClientV2(config, dropboxSettings.getAccessToken());

        try (BufferedInputStream bufferedInputStream = new BufferedInputStream(in);

             ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(byteArrayOutputStream)
        ) {
            long currentBackupPart = 0;
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
        } catch (DbxException | IOException ex) {
            throw new RuntimeException("Error occurred while uploading backup to Dropbox", ex);
        }
    }

    /**
     * Downloads backup from Dropbox
     */
    public InputStream downloadBackup(@NotNull StorageSettings storageSettings, @NotNull String backupName) {
        String backupFolderPath = "/" + backupName;
        DbxRequestConfig config = DbxRequestConfig.newBuilder("dbBackupDownloader").build();
        DropboxSettings dropboxSettings = storageSettings.getDropboxSettings().orElseThrow(() -> new RuntimeException(
                "Can't download backup from Dropbox storage: Missing Dropbox Settings"));
        DbxClientV2 dbxClient = new DbxClientV2(config, dropboxSettings.getAccessToken());

        long filesCount;
        try {
            ListFolderResult listFolderResult = dbxClient.files().listFolder(backupFolderPath);
            List<Metadata> listFolderMetadata = listFolderResult.getEntries();
            filesCount = listFolderMetadata.size();
        } catch (DbxException ex) {
            throw new RuntimeException(
                    String.format("Error listing Dropbox backup folder. Backup folder: %s", backupFolderPath), ex);
        }

        try {
            PipedOutputStream out = new PipedOutputStream();
            PipedInputStream in = new PipedInputStream();
            in.connect(out);

            Thread backupDownloader = new Thread(new BackupDownloader(out, dbxClient, backupFolderPath, backupName, filesCount));
            backupDownloader.start();

            return in;
        } catch (IOException ex) {
            throw new RuntimeException("Error occurred while initializing backup downloading from Dropbox", ex);
        }
    }

    @Override
    public void deleteBackup(StorageSettings storageSettings, String backupName) {
        String backupFolderPath = "/" + backupName;
        DbxRequestConfig config = DbxRequestConfig.newBuilder("dbBackupDeleted").build();
        DropboxSettings dropboxSettings = storageSettings.getDropboxSettings().orElseThrow(() -> new RuntimeException(
                "Can't delete backup from Dropbox storage: Missing Dropbox Settings"));
        DbxClientV2 dbxClient = new DbxClientV2(config, dropboxSettings.getAccessToken());

        try {
            dbxClient.files().deleteV2(backupFolderPath);
        } catch (DbxException ex) {
            throw new RuntimeException(
                    String.format("Error deleting backup from Dropbox. Backup folder: %s", backupFolderPath), ex);
        }
    }

    private class BackupDownloader implements Runnable {
        private OutputStream out;

        private DbxClientV2 dbxClient;

        private long filesCount;

        private String backupFolderPath;

        private String backupName;

        BackupDownloader(OutputStream out, DbxClientV2 dbxClient, String backupFolderPath,
                         String backupName, long filesCount) {
            this.out = out;
            this.dbxClient = dbxClient;
            this.backupFolderPath = backupFolderPath;
            this.backupName = backupName;
            this.filesCount = filesCount;
        }

        public void run() {
            try {
                logger.info("Total files in backup folder on Dropbox: {}. Backup folder: {}", filesCount, backupFolderPath);
                String currentFile;
                for (long currentBackupPart = 0; currentBackupPart < filesCount; currentBackupPart++) {
                    currentFile = getCurrentFilePartAsAbsolutePath(backupFolderPath, backupName, currentBackupPart);
                    dbxClient.files().downloadBuilder(currentFile).download(out);
                }
                out.close();
            } catch (DbxException | IOException ex) {
                throw new RuntimeException(
                        String.format("Error occurred while downloading backup from Dropbox. Backup folder: %s",
                                backupFolderPath), ex);
            }
        }
    }
}
