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
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class DropboxBinaryStorage implements BinaryStorage {
    private DbxClientV2 dbxClient;

    private static final Logger logger = LoggerFactory.getLogger(DropboxBinaryStorage.class);

    private String backupFolder;

    private String backupName;

    private long currentBackupPart;

    public DropboxBinaryStorage(StorageSettings storageSettings, String backupName) {
        this.backupName = backupName;
        this.backupFolder = this.backupName;
        currentBackupPart = 0;
        DbxRequestConfig config = DbxRequestConfig.newBuilder("dbBackup").build();
        DropboxSettings dropboxSettings = storageSettings.getDropboxSettings().orElseThrow(() -> new RuntimeException("Can't " +
                "construct Dropbox Text Storage: Missing Dropbox Settings"));
        dbxClient = new DbxClientV2(config, dropboxSettings.getAccessToken());
    }

    private static class ZipFileWriter implements Runnable {
        byte[] data;
        ZipOutputStream zipOutputStream;
        String entryName;

        public ZipFileWriter(ZipOutputStream zipOutputStream, byte[] data, String entryName) {
            this.zipOutputStream = zipOutputStream;
            this.data = data;
            this.entryName = entryName;
        }

        @Override
        public void run() {
            try {
                zipOutputStream.putNextEntry(new ZipEntry(entryName));
                zipOutputStream.write(data);

                zipOutputStream.closeEntry();
                zipOutputStream.close();
            } catch (IOException ex) {
                throw new RuntimeException("Error uploading backup to Dropbox", ex);
            }
        }
    }

    @Override
    public void uploadBackup(byte[] data) {
        try {
            PipedOutputStream pipedOutputStream = new PipedOutputStream();

            PipedInputStream pipedInputStream = new PipedInputStream();
            pipedInputStream.connect(pipedOutputStream);

            ZipOutputStream zipOutputStream = new ZipOutputStream(pipedOutputStream);

            String entryName = backupName + "_part" + currentBackupPart;
            Thread zipFileWriterThread = new Thread(new ZipFileWriter(zipOutputStream, data, entryName));
            zipFileWriterThread.start();

            String currentFile = entryName + ".zip";
            logger.info("Uploading a new data to Dropbox. Current file: {}", currentFile);
            dbxClient.files().uploadBuilder("/" + backupFolder + "/" + currentFile).uploadAndFinish(pipedInputStream);
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
                    String currentFile = "/" + backupFolder + "/" + backupName + "_part" + currentFileCount + ".zip";
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

        long filesCount = 0;
        try {
            ListFolderResult listFolderResult = dbxClient.files().listFolder("/" + backupFolder);
            List<Metadata> listFolderMetadata = listFolderResult.getEntries();
            filesCount = listFolderMetadata.size();
        } catch (DbxException ex) {
            throw new RuntimeException("Error listing backup folder", ex);
        }

        logger.info("Files in backup folder: {}", filesCount);

        try {
            PipedOutputStream out = new PipedOutputStream();
            PipedInputStream in = new PipedInputStream();
            in.connect(out);

            Thread backupDownloader = new Thread(new BackupDownloader(out, filesCount));
            backupDownloader.start();

            return new ZipInputStream(in);
        } catch (IOException ex) {
            throw new RuntimeException("Error connecting input stream to backup download output stream", ex);
        }
    }
}
