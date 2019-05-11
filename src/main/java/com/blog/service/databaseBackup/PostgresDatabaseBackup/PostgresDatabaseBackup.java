package com.blog.service.databaseBackup.PostgresDatabaseBackup;

import com.blog.entities.database.DatabaseSettings;
import com.blog.service.databaseBackup.DatabaseBackup;
import com.blog.service.databaseBackup.PostgresDatabaseBackup.Errors.InternalPostgresToolError;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * This class class handles PostgreSQL database backups
 */
@Service
public class PostgresDatabaseBackup implements DatabaseBackup {
    private static final Logger logger = LoggerFactory.getLogger(PostgresDatabaseBackup.class);
    private static ErrorCallback errorCallback;
    private String pgDumpToolPath;
    private String psqlToolPath;

    @Autowired
    public void setErrorCallback(ErrorCallback errorCallback) {
        PostgresDatabaseBackup.errorCallback = errorCallback;
    }

    @Autowired
    public void setPgDumpToolPath(String pgDumpToolPath) {
        this.pgDumpToolPath = pgDumpToolPath;
    }

    @Autowired
    public void setPsqlToolPath(String psqlToolPath) {
        this.psqlToolPath = psqlToolPath;
    }

    private ArrayList<String> addCommandParam(ArrayList<String> command, String paramName, String paramValue) {
        command.add(paramName);
        if (paramValue != null && !paramValue.isEmpty()) {
            command.add(paramValue);
        }
        return command;
    }

    private ProcessBuilder buildProcess(List<String> command, DatabaseSettings databaseSettings) {
        ProcessBuilder pb;

        pb = new ProcessBuilder(command);
        pb.environment().put("PGUSER", databaseSettings.getLogin());
        pb.environment().put("PGPASSWORD", databaseSettings.getPassword());

        return pb;
    }

    private List<String> buildBackupCommand(DatabaseSettings databaseSettings) {
        ArrayList<String> command = new ArrayList<>();

        command.add(pgDumpToolPath);
        command = addCommandParam(command, "-h", databaseSettings.getHost());
        command = addCommandParam(command, "-p", Integer.toString(databaseSettings.getPort()));
        command = addCommandParam(command, "-F", "p");
        command = addCommandParam(command, "-d", databaseSettings.getName());

        return command;
    }

    private List<String> buildRestoreCommand(DatabaseSettings databaseSettings) {
        ArrayList<String> command = new ArrayList<>();

        command.add(psqlToolPath);
        command = addCommandParam(command, "-h", databaseSettings.getHost());
        command = addCommandParam(command, "-U", databaseSettings.getLogin());
        command = addCommandParam(command, "-p", Integer.toString(databaseSettings.getPort()));
        command = addCommandParam(command, "-d", databaseSettings.getName());

        return command;
    }

    /**
     * Creates PostgreSQL database plan-text backup.
     * Backup is created by <i>pg_dump</i> tool
     * <p>
     * If pg_dump reports about error while executing, InternalPostgresToolError will be thrown
     * In such case, you can find process's stderr messages in the class log
     * <p>
     * Note, that this function returns directly process's stdin stream, that is you will not have to wait for full backup creation.
     * Further reading is performing by processor or storage service.
     *
     * @return input stream, connected to the normal output stream of the process, from which backup can be read
     */
    public InputStream createBackup(@NotNull DatabaseSettings databaseSettings, @NotNull Integer id)
            throws InternalPostgresToolError {
        List<String> backupCommand = buildBackupCommand(databaseSettings);
        logger.info("Creating PostgreSQL backup of database {} hosted on address {}:{}", databaseSettings.getName(),
                databaseSettings.getHost(), databaseSettings.getPort());

        Process process;
        try {
            process = buildProcess(backupCommand, databaseSettings).start();
        } catch (IOException ex) {
            throw new RuntimeException("Error starting PostgreSQL database backup process", ex);
        }

        Thread processStderrReaderThread = new Thread(new ProcessStderrStreamReadWorker(process.getErrorStream(),
                JobType.BACKUP, id));
        processStderrReaderThread.start();

        // we should wait for backup process terminating in separate thread, otherwise
        // waitFor() deadlocks the thread since process's output is not being read and buffer overflows which leads to blocking
        new Thread() {
            public void run() {
                try {
                    logger.info("Waiting for PostgreSQL backup process termination...");
                    int exitVal = process.waitFor();
                    if (exitVal != 0) {
                        errorCallback.onError(new RuntimeException("pg_dump process terminated with error"), id);
                    }

                    logger.info("Waiting for complete reading of backup process's output stream buffer...");

                    InputStream inputStream = process.getInputStream();
                    try {
                        while (inputStream.available() != 0) {
                            Thread.yield();
                        }
                    } catch (IOException ex) {
                        logger.error("Error checking process's output stream for data to be read. Probably stream was closed");
                    }

                    process.destroy();

                    logger.info("PostgreSQL backup process destroyed");
                } catch (InterruptedException ex) {
                    logger.error("Error terminating PostgreSQL backup process", ex);
                }
            }
        }.start();

        return process.getInputStream();
    }

    /**
     * Restores PostgreSQL database plain-text backup.
     * Backup is restored by <i>psql</i> tool.
     * <p>
     * If psql reports about error while executing, InternalPostgresToolError will be thrown
     * In such case, you can find process's stderr messages in the class log
     * <p>
     * Note, that there are two types of possible errors: the one from psql tool executing in separate process,
     * and the second one produced by Java (IO Exception) while writing backup to process output stream
     *
     * @param backupSource the input stream to read backup from
     */
    public void restoreBackup(@NotNull InputStream backupSource, @NotNull DatabaseSettings databaseSettings, @NotNull Integer id)
            throws InternalPostgresToolError {
        List<String> restoreCommand = buildRestoreCommand(databaseSettings);
        logger.info("Restoring PostgreSQL backup to database {} hosted on address {}:{}", databaseSettings.getName(),
                databaseSettings.getHost(), databaseSettings.getPort());

        Process process;
        try {
            process = buildProcess(restoreCommand, databaseSettings).start();
        } catch (IOException ex) {
            throw new RuntimeException("Error starting PostgreSQL database restore process", ex);
        }

        Thread processStderrReaderThread =
                new Thread(new ProcessStderrStreamReadWorker(process.getErrorStream(), JobType.RESTORE, id));
        processStderrReaderThread.start();

        Thread processStdoutReaderThread = new Thread(
                new ProcessStdoutStreamReadWorker(process.getInputStream(), JobType.RESTORE));
        processStdoutReaderThread.start();

        try (
                BufferedReader backupReader = new BufferedReader(new InputStreamReader(backupSource));
                BufferedWriter processOutputWriter = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()))
        ) {
            String currentLine;
            while ((currentLine = backupReader.readLine()) != null) {
                processOutputWriter.write(currentLine + System.lineSeparator());
            }
            // quit from psql
            processOutputWriter.write("\\q" + System.lineSeparator());
            processOutputWriter.flush();
        } catch (IOException ex) {
            throw new RuntimeException("Error occurred while restoring PostgreSQL backup", ex);
        }

        try {
            logger.info("Waiting for PostgreSQL restore process termination...");
            process.waitFor();
        } catch (InterruptedException ex) {
            logger.error("Error terminating PostgreSQL restore process", ex);
            throw new RuntimeException(ex);
        }

        logger.info("Destroying PostgreSQL restore process...");
        process.destroy();

        logger.info("PostgreSQL database backup successfully restored. Restored to database {}", databaseSettings.getName());
    }

    private enum JobType {
        BACKUP,
        RESTORE;

        public String getJobPrefix() {
            switch (this) {
                case BACKUP:
                    return "[BACKUP]";
                case RESTORE:
                    return "[RESTORE]";
                default: {
                    throw new RuntimeException("No such PostgreSQL database backup Job Type");
                }
            }
        }
    }

    private static final class ProcessStdoutStreamReadWorker implements Runnable {
        InputStream outputStream;

        private String STDOUT_PRINT_FORMAT;

        private JobType jobType;

        ProcessStdoutStreamReadWorker(InputStream outputStream, JobType jobType) {
            this.outputStream = outputStream;
            this.jobType = jobType;
            this.STDOUT_PRINT_FORMAT = jobType.getJobPrefix() + " stdout: {}";
        }

        @Override
        public void run() {
            logger.info("Reading process standard output stream...");
            try (
                    BufferedReader outputStreamReader = new BufferedReader(new InputStreamReader(outputStream))
            ) {
                String currentLine;
                while ((currentLine = outputStreamReader.readLine()) != null) {
                    logger.info(STDOUT_PRINT_FORMAT, currentLine);
                }
            } catch (IOException ex) {
                throw new RuntimeException("Error occurred while reading process standard output stream", ex);
            }
        }
    }

    private static final class ProcessStderrStreamReadWorker implements Runnable {
        InputStream errorStream;

        private JobType jobType;

        private String STDERR_PRINT_FORMAT;

        private Integer id;

        private ProcessStderrStreamReadWorker(InputStream errorStream, JobType jobType, Integer id) {
            this.errorStream = errorStream;
            this.jobType = jobType;
            this.id = id;

            this.STDERR_PRINT_FORMAT = jobType.getJobPrefix() + " stderr: {}";
        }

        @Override
        public void run() {
            logger.info("Reading process standard error stream...");
            try (
                    BufferedReader errorStreamReader = new BufferedReader(new InputStreamReader(errorStream))
            ) {
                boolean isErrorOccurred = false;
                String error;
                while ((error = errorStreamReader.readLine()) != null) {
                    isErrorOccurred = true;
                    logger.error(STDERR_PRINT_FORMAT, error);
                }
                if (isErrorOccurred) {
                    errorCallback.onError(new InternalPostgresToolError(String.format(
                            "Error occurred while executing PostgreSQL database job. Job Type: %s. See process's stderr for details",
                            jobType.toString())), id);
                }
            } catch (IOException ex) {
                throw new RuntimeException("Error occurred while reading process standard error stream", ex);
            }
        }
    }
}