package com.example.demo.service.storage;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import com.example.demo.entities.storage.DropboxSettings;
import com.example.demo.entities.storage.StorageSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.List;

public class DropboxBinaryStorage implements BinaryStorage {
    private DbxClientV2 dbxClient;

    private static final Logger logger = LoggerFactory.getLogger(DropboxBinaryStorage.class);

    private String backupFolderPath;

    private String backupName;

    private long currentBackupPart;

    private static final String FILE_EXTENSION = ".dat";

    private static final String FILENAME_TEMPLATE = "%s_part%d";

    public DropboxBinaryStorage(StorageSettings storageSettings, String backupName) {
        this.backupName = backupName;
        this.backupFolderPath = "/" + backupName;
        currentBackupPart = 0;
        DbxRequestConfig config = DbxRequestConfig.newBuilder("dbBackup").build();
        DropboxSettings dropboxSettings = storageSettings.getDropboxSettings().orElseThrow(() -> new RuntimeException(
                "Can't construct Dropbox Text Storage: Missing Dropbox Settings"));
        dbxClient = new DbxClientV2(config, dropboxSettings.getAccessToken());
    }

    private String getCurrentFilePartAsAbsolutePath() {
        String filename = String.format(FILENAME_TEMPLATE, backupName, currentBackupPart);
        return backupFolderPath + "/" + filename + FILE_EXTENSION;
    }

    @Override
    public void uploadBackup(byte[] data) {
        logger.info("Uploading compressed backup chunk to Dropbox into folder {}", backupFolderPath);
        String currentFilePath = getCurrentFilePartAsAbsolutePath();
        try {
            InputStream in = new ByteArrayInputStream(data);
            dbxClient.files().uploadBuilder(currentFilePath).uploadAndFinish(in);
            currentBackupPart++;
        } catch (DbxException | IOException ex) {
            throw new RuntimeException("Error occurred while uploading backup to Dropbox", ex);
        }

        logger.info("Compressed backup chunk successfully saved on Dropbox. Created file: {}",
                currentFilePath.substring(currentFilePath.lastIndexOf("/") + 1));
    }

    private class BackupDownloader implements Runnable {
        OutputStream out;

        long filesCount;

        BackupDownloader(OutputStream out, long filesCount) {
            this.out = out;
            this.filesCount = filesCount;
        }

        public void run() {
            try {
                logger.info("Total files in backup folder on Dropbox: {}. Backup folder: {}", filesCount, backupFolderPath);
                for (currentBackupPart = 0; currentBackupPart < filesCount; currentBackupPart++) {
                    String currentFile = getCurrentFilePartAsAbsolutePath();
                    logger.info("Downloading file [{}]: '{}'...", currentBackupPart,
                            currentFile.substring(currentFile.lastIndexOf("/") + 1));
                    dbxClient.files().downloadBuilder(currentFile).download(out);
                }
                out.close();
                logger.info("Downloading of compressed backup from Dropbox successfully completed. Backup folder: {}", backupFolderPath);
            } catch (DbxException | IOException ex) {
                throw new RuntimeException(
                        String.format("Error occurred while downloading compressed backup from Dropbox. Backup folder: %s",
                                backupFolderPath), ex);
            }
        }
    }

    @Override
    public InputStream downloadBackup() {
        logger.info("Downloading compressed backup from Dropbox. Backup folder: {}", backupFolderPath);

        long filesCount;
        try {
            ListFolderResult listFolderResult = dbxClient.files().listFolder(backupFolderPath);
            List<Metadata> listFolderMetadata = listFolderResult.getEntries();
            filesCount = listFolderMetadata.size();
        } catch (DbxException ex) {
            throw new RuntimeException(String.format("Error listing Dropbox backup folder. Backup folder: %s", backupFolderPath), ex);
        }

        try {
            PipedOutputStream out = new PipedOutputStream();
            PipedInputStream in = new PipedInputStream();
            in.connect(out);

            Thread backupDownloader = new Thread(new BackupDownloader(out, filesCount));
            backupDownloader.start();

            return in;
        } catch (IOException ex) {
            throw new RuntimeException("Error occurred while initializing compressed backup downloading from Dropbox", ex);
        }
    }
}
