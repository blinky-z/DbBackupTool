package com.blog.service.databaseBackup.PostgresDatabaseBackup;

import com.blog.entities.database.DatabaseSettings;
import com.blog.service.ErrorCallbackService;
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
import java.util.concurrent.ExecutorService;

/**
 * Implementation of {@link DatabaseBackup} interface for PostgreSQL.
 */
@Service
public class PostgresDatabaseBackup implements DatabaseBackup {
    private static final Logger logger = LoggerFactory.getLogger(PostgresDatabaseBackup.class);
    private String pgDumpToolPath;
    private String psqlToolPath;

    private ErrorCallbackService errorCallbackService;

    private ExecutorService postgresExecutorService;

    @Autowired
    public void setPgDumpToolPath(String pgDumpToolPath) {
        this.pgDumpToolPath = pgDumpToolPath;
    }

    @Autowired
    public void setPsqlToolPath(String psqlToolPath) {
        this.psqlToolPath = psqlToolPath;
    }

    @Autowired
    public void setErrorCallbackService(ErrorCallbackService errorCallbackService) {
        this.errorCallbackService = errorCallbackService;
    }

    @Autowired
    public void setPostgresExecutorService(ExecutorService postgresExecutorService) {
        this.postgresExecutorService = postgresExecutorService;
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
     * <p>
     * Backup is creating by <i>pg_dump</i> tool.
     * <p>
     * If pg_dump exits with non-zero exit code, {@link InternalPostgresToolError} will be thrown. In such case, you can find process's stderr
     * messages in the log.
     * <p>
     * Note, that this function returns directly process's output stream, that is you will not have to wait for full backup creation and
     * backup will be writing in real time. But if buffer overflows, pg_dump process hangs until there will be free space in output stream's
     * buffer.
     *
     * @return input stream, connected to the output stream of the pg_dump process
     * @implNote {@literal pg_dump} process wil be destroyed only if its output stream was closed to prevent situation when process exits
     * but there are left some data in buffer which will not be available for retrieving after destroying of process. So always ensure that
     * you close returned input stream after getting EOF or exception.
     */
    @NotNull
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

        // in case server will be shutdown while restoring backup
        // if process was already destroyed, then it does nothing
        Runtime.getRuntime().addShutdownHook(new Thread(process::destroyForcibly));

        postgresExecutorService.submit(new ProcessStderrStreamReadWorker(process.getErrorStream(), JobType.BACKUP));

        // we should wait for backup process terminating in separate thread, otherwise
        // waitFor() deadlocks the thread since process's output is not being read and buffer overflows what leads to blocking
        postgresExecutorService.submit(() -> {
            try {
                InputStream inputStream = process.getInputStream();

                int exitVal = process.waitFor();
                logger.debug("PostgreSQL backup creation process exited with value {}", exitVal);
                if (exitVal != 0) {
                    // check if pg_dump exited with error due to closing of its output stream
                    // in such case it means that error or interrupt occurred while handling backup and the according backup stream
                    // was closed, but in fact that stream is a pg_dump's output stream
                    // we don't consider it as error
                    try {
                        inputStream.available();
                    } catch (IOException ex) {
                        // the input stream is the instance of BufferedInputStream on Windows and the instance of ProcessPipeInputStream
                        // which extends BufferedInputStream on Linux
                        // both implementations throws IOException with message "Stream closed" when calling available() on the closed stream
                        if (ex.getMessage().equals("Stream closed")) {
                            process.destroy();
                            return;
                        }
                    }

                    errorCallbackService.onError(new InternalPostgresToolError(
                            "PostgreSQL backup process terminated with error. See process's stderr log for details"), id);
                }

                logger.debug("PostgreSQL backup process terminated. Waiting for full reading of process's output stream buffer");

                try {
                    // we should not close input stream until all data left in stdout buffer will be read
                    // not only we should wait until all data will be read, but also ensure that a reader got EOF and closed the stream
                    // we can know that stream was closed getting an IOException
                    while (true) {
                        inputStream.available();
                        Thread.sleep(5000);
                    }
                } catch (IOException ex) {
                    // can and should happen in case of closing the stream, which means that all data has been read and we can destroy process
                    process.destroy();
                    logger.info("PostgreSQL backup creation process destroyed");
                }
            } catch (InterruptedException ex) {
                // this thread might be interrupted only by the shutdown() method on executor service destroying
                // process will be destroyed by a shutdown hook, so do nothing
            }
        });

        logger.info("PostgreSQL backup creation started. Database: {}", databaseSettings.getName());

        return process.getInputStream();
    }

    /**
     * Restores PostgreSQL database plain-text backup.
     * <p>
     * Backup is restored by <i>psql</i> tool.
     * Restoration is performing in single transaction. If connection times out, then transaction will be canceled and no data will be
     * restored.
     * <p>
     * If psql reports about error while executing, InternalPostgresToolError will be thrown. In such case, you can find process's stderr
     * messages in the log of this class.
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

        postgresExecutorService.submit(new ProcessStderrStreamReadWorker(process.getErrorStream(), JobType.RESTORE));
        postgresExecutorService.submit(new ProcessStdoutStreamReadWorker(process.getInputStream(), JobType.RESTORE));

        // in case server will be shutdown while restoring backup, than we need to discard transaction
        // if process was already normally destroyed, then it does nothing
        Runtime.getRuntime().addShutdownHook(new Thread(process::destroyForcibly));

        try (
                BufferedReader backupReader = new BufferedReader(new InputStreamReader(backupSource));
                BufferedWriter processOutputWriter = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()))
        ) {
            String currentLine;
            try {
                processOutputWriter.write("BEGIN;");
                while ((currentLine = backupReader.readLine()) != null) {
                    processOutputWriter.write(currentLine + System.lineSeparator());
                }
                processOutputWriter.write("COMMIT;");
            } catch (InterruptedIOException ex) {
                logger.error("PostgreSQL backup restoration was interrupted. Rolling back... Database: {}", databaseSettings);

                try {
                    processOutputWriter.write("ROLLBACK;");
                    // quit from psql
                    processOutputWriter.write("\\q" + System.lineSeparator());
                    processOutputWriter.flush();

                    process.destroy();
                } catch (IOException ex_) {
                    logger.error("I/O error rolling back changes. Database: {}", databaseSettings, ex_);
                    // initiate connection timeout and rollback destroying process
                    process.destroyForcibly();
                }

                return;
            }
            // quit from psql
            processOutputWriter.write("\\q" + System.lineSeparator());
            processOutputWriter.flush();
        } catch (IOException ex) {
            throw new RuntimeException("Error occurred while restoring PostgreSQL backup", ex);
        }

        try {
            logger.info("Waiting for PostgreSQL restore process termination...");
            // should return immediately as we quited from psql
            int exitVal = process.waitFor();
            if (exitVal != 0) {
                throw new InternalPostgresToolError(
                        "PostgreSQL restore process terminated with error. See process's stderr log for details");
            }
        } catch (InterruptedException ignore) {
            // should not happen usually
            // can happen if interrupted too late, when backup was already restored. Ignore in such case
        } finally {
            process.destroy();
        }

        logger.info("PostgreSQL database backup successfully restored. Database: {}", databaseSettings.getName());
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


    private class ProcessStderrStreamReadWorker implements Runnable {
        private InputStream in;

        private String STDERR_PRINT_FORMAT;

        ProcessStderrStreamReadWorker(InputStream in, JobType jobType) {
            this.in = in;
            this.STDERR_PRINT_FORMAT = jobType.getJobPrefix() + " stderr: {}";
        }

        @Override
        public void run() {
            try (
                    BufferedReader errorStreamReader = new BufferedReader(new InputStreamReader(in))
            ) {
                String error;
                while ((error = errorStreamReader.readLine()) != null) {
                    logger.error(STDERR_PRINT_FORMAT, error);
                }
            } catch (IOException ex) {
                throw new RuntimeException("Error occurred while reading process standard error stream", ex);
            }
        }
    }

    private class ProcessStdoutStreamReadWorker implements Runnable {
        private InputStream out;

        private String STDOUT_PRINT_FORMAT;

        ProcessStdoutStreamReadWorker(InputStream out, JobType jobType) {
            this.out = out;
            this.STDOUT_PRINT_FORMAT = jobType.getJobPrefix() + " stdout: {}";
        }

        @Override
        public void run() {
            try (
                    BufferedReader outputStreamReader = new BufferedReader(new InputStreamReader(out))
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

}