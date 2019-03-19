package com.example.demo.storage;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import com.example.demo.DbBackup;
import com.example.demo.settings.DatabaseSettings;
import com.example.demo.settings.UserSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Component
public class DropboxTextStorage implements TextStorage {
    @Autowired
    private UserSettings userSettings;

    @Autowired
    private DatabaseSettings databaseSettings;

    private DbxClientV2 client;

    private static final Logger logger = LoggerFactory.getLogger(DbBackup.class);

    private String backupFolder;

    private String backupName;

    private long currentBackupPart;

    private void init() {
        SimpleDateFormat date = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss");
        String dateAsString = date.format(new Date());
        backupName = "backup_" +
                databaseSettings.getDatabaseName() + "_" + dateAsString;
        backupFolder = backupName;
        currentBackupPart = 0;
    }

    @Override
    public void saveBackup(String data) {
        if (backupName == null) {
            init();
        }
        DbxRequestConfig config = DbxRequestConfig.newBuilder("dbBackup").build();
        client = new DbxClientV2(config, userSettings.getDropboxAccessToken());

        InputStream in = new ByteArrayInputStream(data.getBytes(Charset.defaultCharset()));
        try {
            String currentFile = backupName + "_part" + currentBackupPart + ".data";
            logger.info("Uploading a new file to Dropbox: {}", currentFile);
            client.files().uploadBuilder("/" + backupFolder + "/" + currentFile).uploadAndFinish(in);
            currentBackupPart++;
        } catch (DbxException | IOException ex) {
            throw new RuntimeException("Error uploading backup file to Dropbox", ex);
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
                    client.files().downloadBuilder(currentFile).download(out);
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
        DbxRequestConfig config = DbxRequestConfig.newBuilder("dbBackup").build();
        client = new DbxClientV2(config, userSettings.getDropboxAccessToken());

        System.out.println("Downloading backup from Dropbox...");

        PipedOutputStream out = new PipedOutputStream();

        long filesCount = 0;
        try {
            ListFolderResult listFolderResult = client.files().listFolder("/" + backupFolder);
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
