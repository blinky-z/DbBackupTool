package com.blog.manager;

import com.blog.entities.backup.BackupProperties;
import com.blog.entities.storage.StorageSettings;
import com.blog.entities.storage.StorageType;
import com.blog.service.ErrorCallbackService;
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
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * This class provides API to upload, download and delete backups.
 */
@Component
public class BackupLoadManager {
    private static final Logger logger = LoggerFactory.getLogger(BackupLoadManager.class);

    private ExecutorService backupLoadManagerExecutorService;

    private StorageSettingsManager storageSettingsManager;

    private DropboxStorage dropboxStorage;

    private FileSystemStorage fileSystemStorage;

    private ErrorCallbackService errorCallbackService;

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

    @Autowired
    public void setErrorCallbackService(ErrorCallbackService errorCallbackService) {
        this.errorCallbackService = errorCallbackService;
    }

    /**
     * Uploads backup.
     * <p>
     * Uploading to multiple storages is performing in parallel.
     *
     * @param backupStream     InputStream from which backup can be read
     * @param backupProperties pre-created BackupProperties of backup that should be uploaded to storage
     * @param id               task ID
     * @implNote When uploading a backup, each storage uploading is performing in its own thread.
     * <p>
     * If any upload task reports about exception (either throwing it from the main thread or using {@link ErrorCallbackService}), all
     * tasks will be canceled.
     * <p>
     * If task reports about error using {@link ErrorCallbackService}, this task also will be canceled to let main thread of this task
     * stop the work.
     */
    public void uploadBackup(@NotNull InputStream backupStream, @NotNull BackupProperties backupProperties, @NotNull Integer id) {
        Objects.requireNonNull(backupStream);
        Objects.requireNonNull(backupProperties);
        Objects.requireNonNull(id);

        List<String> storageSettingsNameList = backupProperties.getStorageSettingsNameList();
        List<StorageSettings> storageSettingsList = new ArrayList<>();

        for (String storageSettingsName : storageSettingsNameList) {
            storageSettingsList.add(storageSettingsManager.findById(storageSettingsName).orElseThrow(() ->
                    new RuntimeException("Can't upload backup: no such storage settings with name " + storageSettingsName)));
        }

        String backupName = backupProperties.getBackupName();

        List<Runnable> runnables = new ArrayList<>();
        List<PipedOutputStream> pipedOutputStreamList = new ArrayList<>();
        List<PipedInputStream> pipedInputStreamList = new ArrayList<>();
        for (StorageSettings storageSettings : storageSettingsList) {
            PipedInputStream pipedInputStream = new PipedInputStream();
            PipedOutputStream pipedOutputStream = new PipedOutputStream();

            pipedInputStreamList.add(pipedInputStream);
            pipedOutputStreamList.add(pipedOutputStream);

            try {
                pipedOutputStream.connect(pipedInputStream);
            } catch (IOException ex) {
                for (PipedInputStream pipedInputStream_ : pipedInputStreamList) {
                    try {
                        pipedInputStream_.close();
                    } catch (IOException ignore) {
                    }
                }
                for (PipedOutputStream pipedOutputStream_ : pipedOutputStreamList) {
                    try {
                        pipedOutputStream_.close();
                    } catch (IOException ignore) {
                    }
                }

                throw new RuntimeException("Error initializing backup uploading", ex);
            }

            StorageType storageType = storageSettings.getType();
            switch (storageType) {
                case LOCAL_FILE_SYSTEM: {
                    runnables.add(() -> fileSystemStorage.uploadBackup(pipedInputStream, storageSettings, backupName, id));
                    break;
                }
                case DROPBOX: {
                    runnables.add(() -> dropboxStorage.uploadBackup(pipedInputStream, storageSettings, backupName, id));
                    break;
                }
                default: {
                    throw new RuntimeException("Can't upload backup: unknown storage type: " + storageType);
                }
            }
        }

        // we need this variable to know if IO exception occurred because of interrupt or not
        // it is not enough just to check of InterruptedIOException, because exception might occur when writing to the closed stream, which
        // was closed by upload task after interruption
        // also we can wait some time before closing output streams, to make all tasks get InterruptedIOException instead of EOF while
        // calling any blocking I/O method on related PipedInputStream
        AtomicBoolean uploadInterrupted = new AtomicBoolean(false);
        Future uploadTask = backupLoadManagerExecutorService.submit(() -> {
                    try (BackupUploadSplitter backupUploadSplitter =
                                 new BackupUploadSplitter(backupStream, pipedOutputStreamList, uploadInterrupted)) {
                        try {
                            backupUploadSplitter.upload();
                        } catch (IOException ex) {
                            if (!uploadInterrupted.get()) {
                                errorCallbackService.onError(new RuntimeException("Error uploading backup", ex), id);
                            }
                        }
                    } catch (IOException ex) {
                        logger.error("Error releasing stream resources. Backup info: {}", backupProperties, ex);
                    }
                }
        );

        // run upload tasks
        List<Future> futures = new ArrayList<>();
        CountDownLatch countDownLatch = new CountDownLatch(runnables.size());
        for (Runnable runnable : runnables) {
            CompletableFuture future = CompletableFuture
                    .runAsync(runnable, backupLoadManagerExecutorService)
                    .handle((r, ex) -> {
                        // if more than one task completes with an exception, then this if statement has no effect, because tasks was already
                        // canceled and the flag is set
                        if (ex != null) {
                            // we should set flag before canceling the task to avoid situation when context switched right after canceling
                            // but without setting the flag
                            uploadInterrupted.set(true);
                            // if upload already completed, canceling will not have any effect
                            uploadTask.cancel(true);
                            futures.forEach(future_ -> future_.cancel(true));
                        }
                        countDownLatch.countDown();

                        // return either null or an exception
                        return ex;
                    });

            futures.add(future);
        }

        // wait for tasks to complete
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            logger.error("Error uploading backup: upload was canceled. Backup info: {}", backupProperties);

            // we should set flag before canceling the task to avoid situation when context switched right after canceling
            // without setting the flag
            uploadInterrupted.set(true);
            // if upload already completed, canceling will not have any effect
            uploadTask.cancel(true);
            futures.forEach(future_ -> future_.cancel(true));
        }

        // check for exceptions
        for (Future future : futures) {
            try {
                future.get();
            } catch (InterruptedException e) {
                logger.error("Error uploading backup: upload was canceled. Backup info: {}", backupProperties);

                // should not happen usually, because uploading already completed, but user still can interrupt the task
                Thread.currentThread().interrupt();
                return;
            } catch (ExecutionException ex) {
                throw new RuntimeException("Error uploading backup: one of the storages returned an exception", ex);
            }
        }

        logger.info("Backup successfully uploaded. Backup info: {}", backupProperties);
    }

    private final class InterruptDetectInputStream extends InputStream {
        private InputStream in;

        InterruptDetectInputStream(InputStream in) {
            this.in = in;
        }

        @Override
        public int read() throws IOException {
            if (Thread.interrupted()) {
                throw new InterruptedIOException();
            }
            return in.read();
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

        if (downloadedBackup == null) {
            Thread.currentThread().interrupt();
        } else {
            downloadedBackup = new InterruptDetectInputStream(downloadedBackup);
        }
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

        logger.info("Deleting backup... Backup info: {}", backupProperties);

        List<String> storageSettingsNameList = backupProperties.getStorageSettingsNameList();

        List<Runnable> runnableList = new ArrayList<>();
        for (String storageSettingsName : storageSettingsNameList) {
            StorageSettings storageSettings = storageSettingsManager.findById(storageSettingsName).orElseThrow(
                    () -> new RuntimeException("Can't delete backup: no such storage settings with name " + storageSettingsName));
            StorageType storageType = storageSettings.getType();

            String backupName = backupProperties.getBackupName();

            switch (storageType) {
                case LOCAL_FILE_SYSTEM: {
                    runnableList.add(() -> fileSystemStorage.deleteBackup(storageSettings, backupName, id));
                    break;
                }
                case DROPBOX: {
                    runnableList.add(() -> dropboxStorage.deleteBackup(storageSettings, backupName, id));
                    break;
                }
                default: {
                    throw new RuntimeException(String.format("Can't delete backup: unknown storage type: %s", storageType));
                }
            }
        }

        try {
            backupLoadManagerExecutorService.invokeAll(runnableList.stream().map(Executors::callable).collect(Collectors.toList()));
            logger.info("Backup successfully deleted. Backup info: {}", backupProperties);
        } catch (InterruptedException ex) {
            // unfinished tasks automatically canceled here by executor service
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Uploads backup to multiple storages in parallel.
     */
    private static final class BackupUploadSplitter implements AutoCloseable {
        private BufferedInputStream source;
        List<BufferedOutputStream> outputStreamList;
        AtomicBoolean wasInterrupted;

        BackupUploadSplitter(InputStream source, List<PipedOutputStream> outputStreamList, AtomicBoolean wasInterrupted) {
            if (!(source instanceof BufferedInputStream)) {
                this.source = new BufferedInputStream(source);
            } else {
                this.source = (BufferedInputStream) source;
            }
            List<BufferedOutputStream> bufferedOutputStreamList = new ArrayList<>();
            for (OutputStream outputStream : outputStreamList) {
                BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
                bufferedOutputStreamList.add(bufferedOutputStream);
            }
            this.outputStreamList = bufferedOutputStreamList;
            this.wasInterrupted = wasInterrupted;
        }

        void upload() throws IOException {
            final byte[] buffer = new byte[8096];
            int bytesRead;
            while ((bytesRead = source.read(buffer)) != -1) {
                for (BufferedOutputStream outputStream : outputStreamList) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
        }

        @Override
        public void close() throws IOException {
            boolean inputStreamIsClosed = true;
            int outputStreamsClosed = 0;
            boolean exceptionOccurred = false;

            try {
                source.close();
            } catch (IOException ignored) {
                exceptionOccurred = true;
                inputStreamIsClosed = false;
            }

            if (wasInterrupted.get()) {
                // we wait before closing output streams to let all upload tasks handle interrupt and to not get EOF, but get
                // InterruptedIOException while trying to call blocking I/O operation on related PipedInputStream
                try {
                    Thread.sleep(30000);
                } catch (InterruptedException ignored) {
                    // should not happen
                }
            }

            for (BufferedOutputStream outputStream : outputStreamList) {
                try {
                    outputStream.close();
                } catch (IOException ignored) {
                    exceptionOccurred = true;
                    continue;
                }
                outputStreamsClosed++;
            }

            if (exceptionOccurred) {
                throw new IOException(String.format("Error closing streams. Input stream is closed: %s. Output streams closed: [%s/%s]",
                        inputStreamIsClosed, outputStreamsClosed, outputStreamList.size()));
            }
        }
    }
}