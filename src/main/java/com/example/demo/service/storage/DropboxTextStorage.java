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
import java.nio.charset.Charset;
import java.util.List;

public class DropboxTextStorage implements TextStorage {
    private DbxClientV2 dbxClient;

    private static final Logger logger = LoggerFactory.getLogger(DropboxTextStorage.class);

    private String backupFolder;

    private String backupName;

    private long currentBackupPart;

    public DropboxTextStorage(StorageSettings storageSettings, String backupName) {
        this.backupName = backupName;
        this.backupFolder = this.backupName;
        currentBackupPart = 0;
        DbxRequestConfig config = DbxRequestConfig.newBuilder("dbBackup").build();
        DropboxSettings dropboxSettings = storageSettings.getDropboxSettings().orElseThrow(() -> new RuntimeException("Can't " +
                "construct Dropbox Text Storage: Missing Dropbox Settings"));
        dbxClient = new DbxClientV2(config, dropboxSettings.getAccessToken());
    }

    @Override
    public void uploadBackup(String data) {
        InputStream in = new ByteArrayInputStream(data.getBytes(Charset.defaultCharset()));
        try {
            String currentFile = backupName + "_part" + currentBackupPart + ".data";
            logger.info("Uploading a new data to Dropbox. Current file: {}", currentFile);
            dbxClient.files().uploadBuilder("/" + backupFolder + "/" + currentFile).uploadAndFinish(in);
            currentBackupPart++;
        } catch (DbxException | IOException ex) {
            throw new RuntimeException("Error uploading backup to Dropbox", ex);
        }
    }

    private class BackupDownloader implements Runnable {
        OutputStream out;

        long filesCount;

        BackupDownloader(OutputStream out, long filesCount) {
            this.out = out;
            this.filesCount = filesCount;
        }

        public void run() {
            long currentFileCount = 0;
            try {
                for (currentFileCount = 0; currentFileCount < filesCount; currentFileCount++) {
                    String currentFile = "/" + backupFolder + "/" + backupName + "_part" + currentFileCount + ".data";
                    logger.info("Downloading backup file: {}", currentFile);
                    dbxClient.files().downloadBuilder(currentFile).download(out);
                }
            } catch (DbxException | IOException ex) {
                throw new RuntimeException("Error downloading backup from Dropbox", ex);
            }

            try {
                out.close();
            } catch (IOException ex) {
                throw new RuntimeException("Error closing output stream used for downloading backup", ex);
            }
        }
    }

    @Override
    public InputStream downloadBackup() {
        System.out.println("Downloading backup from Dropbox...");

        PipedOutputStream out = new PipedOutputStream();

        long filesCount = 0;
        try {
            ListFolderResult listFolderResult = dbxClient.files().listFolder("/" + backupFolder);
            List<Metadata> listFolderMetadata = listFolderResult.getEntries();
            filesCount = listFolderMetadata.size();
        } catch (DbxException ex) {
            throw new RuntimeException("Error listing backup folder", ex);
        }

        logger.info("Files in backup folder: {}", filesCount);

        PipedInputStream in;
        try {
            in = new PipedInputStream();
            in.connect(out);

            Thread backupDownloader = new Thread(new BackupDownloader(out, filesCount));
            backupDownloader.start();
        } catch (IOException ex) {
            throw new RuntimeException("Error connecting input stream to backup download output stream", ex);
        }

        return in;
    }
}
