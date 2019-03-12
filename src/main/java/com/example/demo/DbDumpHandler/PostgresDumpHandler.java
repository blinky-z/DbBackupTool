package com.example.demo.DbDumpHandler;

import com.example.demo.DbBackup;
import com.example.demo.settings.DatabaseSettings;
import com.example.demo.settings.UserSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * This class allows to work with POSTGRES database backups
 */
@Component
public class PostgresDumpHandler implements DbDumpHandler {
    @Autowired
    private DatabaseSettings databaseSettings;

    @Autowired
    private UserSettings userSettings;

    private static final Logger logger = LoggerFactory.getLogger(DbBackup.class);

    private URI getParsedConnUrl() {
        String jdbcPrefix = "jdbc:";
        String cleanConnUrl = databaseSettings.getUrl().substring(jdbcPrefix.length());

        return URI.create(cleanConnUrl);
    }

    private ArrayList<String> addCommandParam(ArrayList<String> command, String paramName, String paramValue) {
        command.add(paramName);
        if (paramValue != null && !paramValue.isEmpty()) {
            command.add(paramValue);
        }
        return command;
    }

    private Process runProcess(List<String> command) throws IOException {
        Process process;
        ProcessBuilder pb;

        System.out.println("Current url: " + databaseSettings.getUrl());
        System.out.println("Current user: " + databaseSettings.getUsername());
        System.out.println("Current password: " + databaseSettings.getPassword());
        System.out.println("Current db name: " + databaseSettings.getDatabaseName());

        pb = new ProcessBuilder(command);
        pb.environment().put("PGUSER", databaseSettings.getUsername());
        pb.environment().put("PGPASSWORD", databaseSettings.getPassword());
        process = pb.start();

        return process;
    }

    private List<String> getBackupCommand() {
        ArrayList<String> command = new ArrayList<>();

        URI connUrl = getParsedConnUrl();

        SimpleDateFormat date = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss-SS");
        String dateAsString = date.format(new Date());

        command.add("pg_dump");
        command = addCommandParam(command, "-h", connUrl.getHost());
        command = addCommandParam(command, "-p", Integer.toString(connUrl.getPort()));
        command = addCommandParam(command, "-F", "p");
        command = addCommandParam(command, "-d", databaseSettings.getDatabaseName());

        return command;
    }

    private List<String> getRestoreCommand(InputStream dump) {
        ArrayList<String> command = new ArrayList<>();

        URI connUrl = getParsedConnUrl();

        command.add("psql");
        command = addCommandParam(command, "-h", connUrl.getHost());
        command = addCommandParam(command, "-U", databaseSettings.getUsername());
        command = addCommandParam(command, "-p", Integer.toString(connUrl.getPort()));
        command = addCommandParam(command, "-d", databaseSettings.getDatabaseName());

        return command;
    }

    private class ProcessStandartOutputStreamReader implements Runnable {
        InputStream outputStream;

        ProcessStandartOutputStreamReader(InputStream outputStream) {
            this.outputStream = outputStream;
        }

        public void run() {
            logger.info("Reading process standard output stream...");
            try (
                    BufferedReader outputStreamReader = new BufferedReader(new InputStreamReader(outputStream))
            ) {
                String currentLine;
                while ((currentLine = outputStreamReader.readLine()) != null) {
                    logger.info("stdout: " + currentLine);
                }
            } catch (IOException ex) {
                throw new RuntimeException("Error occurred while reading process standard output stream");
            }
        }
    }

    private class ProcessStandartErrorStreamReader implements Runnable {
        InputStream errorStream;

        ProcessStandartErrorStreamReader(InputStream errorStream) {
            this.errorStream = errorStream;
        }

        public void run() {
            logger.info("Reading process standard error stream...");
            try (
                    BufferedReader errorStreamReader = new BufferedReader(new InputStreamReader(errorStream))
            ) {
                boolean isErrorOccurred = false;
                String error;
                while ((error = errorStreamReader.readLine()) != null) {
                    isErrorOccurred = true;
                    logger.error("stderr: " + error);
                }
                if (isErrorOccurred) {
                    throw new RuntimeException("Error occurred while creating/restoring backup");
                }
            } catch (IOException ex) {
                throw new RuntimeException("Error occurred while reading process standard error stream");
            }
        }
    }

    /**
     * Creates POSTGRES database backup.
     * Backup is creating by <i>pg_dump</i> tool.
     *
     * @return input stream, connected to the normal output stream of the process.
     */
    public InputStream createDbDump() {
        List<String> backupCommand = getBackupCommand();
        logger.info("Executing backup command: {}", backupCommand.toString());

        try {
            Process process = runProcess(backupCommand);

            Thread processErrorStreamReader = new Thread(new ProcessStandartErrorStreamReader(process.getErrorStream()));
            processErrorStreamReader.start();

            logger.info("Database backup successfully created. Database: {}", databaseSettings.getDatabaseName());

            return process.getInputStream();
        } catch (IOException ex) {
            throw new RuntimeException("Error occurred while creating postgres database dump", ex);
        }
    }

    /**
     * Restores POSTGRES database backup.
     * Backup is restoring by <i>psql</i> tool.
     *
     * @param dump input stream, that contains the plain-text backup.
     */
    public void restoreDbDump(InputStream dump) {
        try {
            List<String> restoreCommand = getRestoreCommand(dump);
            logger.info("Executing restore command: {}", restoreCommand.toString());

            Process process = runProcess(restoreCommand);

            Thread processErrorStreamReader = new Thread(new ProcessStandartErrorStreamReader(process.getErrorStream()));
            processErrorStreamReader.start();

            Thread processOutputStreamReader = new Thread(new ProcessStandartOutputStreamReader(process.getInputStream()));
            processOutputStreamReader.start();

            try (
                    BufferedReader dumpStreamReader = new BufferedReader(new InputStreamReader(dump));
                    BufferedWriter processOutputWriter = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()))
            ) {
                String currentLine;
                while ((currentLine = dumpStreamReader.readLine()) != null) {
                    processOutputWriter.write(currentLine + System.getProperty("line.separator"));
                }
                processOutputWriter.write("\\q\n");
            }

            process.waitFor();
            process.destroy();

            logger.info("Database successfully restored. Database: {}", databaseSettings.getDatabaseName());
        } catch (IOException | InterruptedException ex) {
            throw new RuntimeException("Error occurred while restoring postgres database dump", ex);
        }
    }
}