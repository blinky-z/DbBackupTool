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

public class DropboxStorage implements Storage {
    private DbxClientV2 dbxClient;

    private static final Logger logger = LoggerFactory.getLogger(DropboxStorage.class);

    private String backupFolderPath;

    private String backupName;

    private long currentBackupPart;

    private static final String FILE_EXTENSION = ".dat";

    private static final String FILENAME_TEMPLATE = "%s_part%d";

    public DropboxStorage(StorageSettings storageSettings, String backupName) {
        this.backupName = backupName;
        this.backupFolderPath = "/" + backupName;
        currentBackupPart = 0;
        DbxRequestConfig config = DbxRequestConfig.newBuilder("dbBackup").build();
        DropboxSettings dropboxSettings = storageSettings.getDropboxSettings().orElseThrow(() -> new RuntimeException(
                "Can't construct Dropbox storage: Missing Settings"));
        logger.info("Constructing new Dropbox Storage with provided settings: {}", dropboxSettings);
        dbxClient = new DbxClientV2(config, dropboxSettings.getAccessToken());
    }

    private String getCurrentFilePartAsAbsolutePath() {
        String filename = String.format(FILENAME_TEMPLATE, backupName, currentBackupPart);
        return backupFolderPath + "/" + filename + FILE_EXTENSION;
    }


    /**
     * Saves backup on Dropbox
     *
     * @param in the input stream to read backup from
     */
    @Override
    public void uploadBackup(InputStream in) {
        logger.info("Uploading backup to Dropbox. Backup folder: {}", backupFolderPath);

        try (BufferedInputStream bufferedInputStream = new BufferedInputStream(in);
             ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(byteArrayOutputStream)
        ) {
            long maxChunkSize = 64L * 1024 * 1024;
            int bytesRead;
            long currentChunkSize = 0;

            byte[] buffer = new byte[64 * 1024];
            while ((bytesRead = bufferedInputStream.read(buffer)) != -1) {
                currentChunkSize += bytesRead;
                bufferedOutputStream.write(buffer, 0, bytesRead);

                if (currentChunkSize >= maxChunkSize) {
                    bufferedOutputStream.flush();
                    String currentFilePath = getCurrentFilePartAsAbsolutePath();
                    dbxClient.files().uploadBuilder(currentFilePath).uploadAndFinish(
                            new ByteArrayInputStream(byteArrayOutputStream.toByteArray()));
                    currentBackupPart++;

                    byteArrayOutputStream.reset();
                    currentChunkSize = 0;
                }
            }
            if (currentChunkSize != 0) {
                bufferedOutputStream.flush();
                String currentFilePath = getCurrentFilePartAsAbsolutePath();
                dbxClient.files().uploadBuilder(currentFilePath).uploadAndFinish(
                        new ByteArrayInputStream(byteArrayOutputStream.toByteArray()));
                byteArrayOutputStream.reset();
            }
        } catch (DbxException | IOException ex) {
            throw new RuntimeException("Error occurred while uploading binary backup to Dropbox", ex);
        }

        logger.info("Backup successfully saved on Dropbox. Backup folder: {}", backupFolderPath);
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
                logger.info("Downloading backup from Dropbox completed. Backup folder: {}", backupFolderPath);
            } catch (DbxException | IOException ex) {
                throw new RuntimeException(
                        String.format("Error occurred while downloading backup from Dropbox. Backup folder: %s",
                                backupFolderPath), ex);
            }
        }
    }

    /**
     * Downloads backup from the file system.
     *
     * @return input stream, from which backup can be read
     */
    public InputStream downloadBackup() {
        logger.info("Downloading backup from Dropbox. Backup folder: {}", backupFolderPath);

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
            throw new RuntimeException("Error occurred while initializing backup downloading from Dropbox", ex);
        }
    }
}
