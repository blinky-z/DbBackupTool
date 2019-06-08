package com.blog.manager;

import com.blog.entities.backup.BackupProperties;
import com.blog.entities.storage.StorageSettings;
import com.blog.entities.storage.StorageType;
import com.blog.service.storage.DropboxStorage;
import com.blog.service.storage.FileSystemStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * This class provides API to upload and download backups.
 */
@Component
public class BackupLoadManager {
    private static final Logger logger = LoggerFactory.getLogger(BackupLoadManager.class);

    private ExecutorService backupLoadManagerExecutorService;

    private StorageSettingsManager storageSettingsManager;

    private DropboxStorage dropboxStorage;

    private FileSystemStorage fileSystemStorage;

    @Autowired
    public void setBackupLoadManagerExecutorService(ExecutorService backupLoadManagerExecutorService) {
        this.backupLoadManagerExecutorService = backupLoadManagerExecutorService;
    }

    @Autowired
    public void setStorageSettingsManager(StorageSettingsManager storageSettingsManager) {
        this.storageSettingsManager = storageSettingsManager;
    }

    @Autowired
    public void setDropboxStorage(DropboxStorage dropboxStorage) {
        this.dropboxStorage = dropboxStorage;
    }

    @Autowired
    public void setFileSystemStorage(FileSystemStorage fileSystemStorage) {
        this.fileSystemStorage = fileSystemStorage;
    }

    /**
     * Uploads backup.
     *
     * @param backupStream     InputStream from which backup can be read
     * @param backupProperties pre-created BackupProperties of backup that should be uploaded to storage
     * @param id               backup upload task ID
     */
    public void uploadBackup(@NotNull InputStream backupStream, @NotNull BackupProperties backupProperties, @NotNull Integer id) {
        Objects.requireNonNull(backupStream);
        Objects.requireNonNull(backupProperties);
        Objects.requireNonNull(id);

        List<String> storageSettingsNameList = backupProperties.getStorageSettingsNameList();
        List<StorageSettings> storageSettingsList = new ArrayList<>();

        for (String storageSettingsName : storageSettingsNameList) {
            storageSettingsList.add(storageSettingsManager.findById(storageSettingsName).orElseThrow(() ->
                    new RuntimeException(String.format("Can't upload backup. Missing storage settings with name %s", storageSettingsName))));
        }

        String backupName = backupProperties.getBackupName();

        List<Future> backupLoadTasks = new ArrayList<>();
        List<PipedOutputStream> pipedOutputStreamList = new ArrayList<>();
        for (StorageSettings storageSettings : storageSettingsList) {
            StorageType storageType = storageSettings.getType();
            logger.info("Uploading backup to {}. Backup name: {}", storageType, backupName);

            PipedInputStream pipedInputStream = new PipedInputStream();
            PipedOutputStream pipedOutputStream = new PipedOutputStream();
            try {
                pipedOutputStream.connect(pipedInputStream);
            } catch (IOException ex) {
                throw new RuntimeException("Error initializing backup loading", ex);
            }

            pipedOutputStreamList.add(pipedOutputStream);

            switch (storageType) {
                case LOCAL_FILE_SYSTEM: {
                    backupLoadTasks.add(backupLoadManagerExecutorService.submit(() ->
                            fileSystemStorage.uploadBackup(pipedInputStream, storageSettings, backupName, id)));
                    break;
                }
                case DROPBOX: {
                    backupLoadTasks.add(backupLoadManagerExecutorService.submit(() ->
                            dropboxStorage.uploadBackup(pipedInputStream, storageSettings, backupName, id)));
                    break;
                }
                default: {
                    throw new RuntimeException(String.format("Can't upload backup. Unknown storage type: %s", storageType));
                }
            }
        }

        try (BufferedInputStream bufferedInputStream = new BufferedInputStream(backupStream);
             BackupLoadSplitter backupLoadSplitter = new BackupLoadSplitter(pipedOutputStreamList)) {
            final byte[] buffer = new byte[4096];
            int bytesRead;
            try {
                while ((bytesRead = bufferedInputStream.read(buffer)) != -1) {
                    backupLoadSplitter.writeChunk(buffer, bytesRead);
                }
            } catch (IOException ex) {
                logger.error("Error uploading backup. Cancelling all tasks", ex);
                // backup can't be uploaded fully, so notify all upload tasks about error
                backupLoadTasks.forEach(task_ -> task_.cancel(true));

                throw new RuntimeException("Error uploading backup", ex);
            }
        } catch (IOException ex) {
            logger.error("Error closing streams", ex);
        }

        for (Future task : backupLoadTasks) {
            try {
                task.get();
            } catch (InterruptedException ignore) {
                // we cancel all tasks to notify them that general backup loading task was canceled
                // interrupt flag is not cleared
                backupLoadTasks.forEach(task_ -> task_.cancel(true));
                break;
            } catch (ExecutionException ex) {
                // we cancel all tasks because backup can not be fully uploaded to all storages
                backupLoadTasks.forEach(task_ -> task_.cancel(true));
                throw new RuntimeException("Error uploading backup. One of the storages returned an exception", ex);
            }
        }
    }

    /**
     * Downloads backup.
     *
     * @param backupName          identifier of the backup
     * @param storageSettingsName identifier of {@link StorageSettings} where backup is stored
     * @param id                  backup restoration task ID
     * @return downloaded backup as {@literal InputStream}
     */
    @Nullable
    public InputStream downloadBackup(@NotNull String backupName, @NotNull String storageSettingsName, @NotNull Integer id) {
        Objects.requireNonNull(backupName);
        Objects.requireNonNull(storageSettingsName);
        Objects.requireNonNull(id);

        logger.info("Downloading backup... Backup name: {}", backupName);

        StorageSettings storageSettings = storageSettingsManager.findById(storageSettingsName).orElseThrow(
                () -> new RuntimeException("Can't download backup: no such storage settings with name " + storageSettingsName));
        StorageType storageType = storageSettings.getType();

        InputStream downloadedBackup;
        switch (storageType) {
            case LOCAL_FILE_SYSTEM: {
                downloadedBackup = fileSystemStorage.downloadBackup(storageSettings, backupName, id);
                break;
            }
            case DROPBOX: {
                downloadedBackup = dropboxStorage.downloadBackup(storageSettings, backupName, id);
                break;
            }
            default: {
                throw new RuntimeException(String.format("Can't download backup. Unknown storage type: %s", storageType));
            }
        }
        logger.info("Backup successfully downloaded from {}. Backup name: {}", storageType, backupName);

        return downloadedBackup;
    }

    /**
     * Deletes backup.
     *
     * @param backupProperties BackupProperties of created backup
     * @param id               backup deletion task ID
     */
    public void deleteBackup(@NotNull BackupProperties backupProperties, @NotNull Integer id) {
        Objects.requireNonNull(backupProperties);

        logger.info("Deleting backup... Backup properties: {}", backupProperties);

        List<String> storageSettingsNameList = backupProperties.getStorageSettingsNameList();

        List<Runnable> backupDeletionRunnableList = new ArrayList<>();
        for (String storageSettingsName : storageSettingsNameList) {
            StorageSettings storageSettings = storageSettingsManager.findById(storageSettingsName).orElseThrow(
                    () -> new RuntimeException("Can't delete backup: no such storage settings with name " + storageSettingsName));
            StorageType storageType = storageSettings.getType();

            String backupName = backupProperties.getBackupName();

            switch (storageType) {
                case LOCAL_FILE_SYSTEM: {
                    backupDeletionRunnableList.add(() -> fileSystemStorage.deleteBackup(storageSettings, backupName, id));
                    break;
                }
                case DROPBOX: {
                    backupDeletionRunnableList.add(() -> dropboxStorage.deleteBackup(storageSettings, backupName, id));
                    break;
                }
                default: {
                    throw new RuntimeException(String.format("Can't delete backup. Unknown storage type: %s", storageType));
                }
            }
        }

        try {
            backupLoadManagerExecutorService.invokeAll(
                    backupDeletionRunnableList.stream().map(Executors::callable).collect(Collectors.toList()));
        } catch (InterruptedException ex) {
            // unfinished deletion tasks automatically canceled here by executor service
            // interrupt flag is not cleared
        }
    }

    private class BackupLoadSplitter implements AutoCloseable {
        List<BufferedOutputStream> outputStreamList;

        BackupLoadSplitter(List<PipedOutputStream> outputStreamList) {
            List<BufferedOutputStream> list = new ArrayList<>();
            for (OutputStream outputStream : outputStreamList) {
                BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
                list.add(bufferedOutputStream);
            }
            this.outputStreamList = list;
        }

        void writeChunk(byte[] buffer, int bytesToWrite) throws IOException {
            for (BufferedOutputStream outputStream : outputStreamList) {
                outputStream.write(buffer, 0, bytesToWrite);
            }
        }

        @Override
        public void close() throws IOException {
            int streamsClosed = 0;
            boolean exceptionOccurred = false;

            for (BufferedOutputStream outputStream : outputStreamList) {
                try {
                    outputStream.close();
                } catch (IOException ignored) {
                    exceptionOccurred = true;
                    continue;
                }
                streamsClosed++;
            }

            if (exceptionOccurred) {
                throw new IOException(
                        String.format("Error closing streams. Streams closed: [%s/%s]", streamsClosed, outputStreamList.size()));
            }
        }
    }
}