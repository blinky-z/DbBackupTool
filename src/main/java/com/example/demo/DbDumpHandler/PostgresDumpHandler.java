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
        pb.environment().put("LC_MESSAGES", "English");
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
        command = addCommandParam(command, "-f", userSettings.getBackupDir() + File.separator +
                "backup_" + databaseSettings.getDatabaseName() + "_" + dateAsString + ".dump");

        return command;
    }

    private List<String> getRestoreCommand(InputStream dump) {
        SimpleDateFormat date = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss-SS");
        String dateAsString = date.format(new Date());

        File backupFile = new File(userSettings.getBackupDir()/*System.getProperty("java.io.tmpdir")*/ + File.separator +
                "backup_" + databaseSettings.getDatabaseName() + "_" + dateAsString + ".dump");

        try (
                BufferedReader dumpStreamReader = new BufferedReader(new InputStreamReader(dump));
                BufferedWriter backupFileWriter = new BufferedWriter(new FileWriter(backupFile))
        ) {
            String currentLine;
            while ((currentLine = dumpStreamReader.readLine()) != null) {
                backupFileWriter.write(currentLine);
                backupFileWriter.write("\n");
            }
        } catch (IOException ex) {
            throw new RuntimeException("Error occurred while creating backup file to restore from psql");
        }

        ArrayList<String> command = new ArrayList<>();

        URI connUrl = getParsedConnUrl();

        command.add("psql");
        command = addCommandParam(command, "-h", connUrl.getHost());
        command = addCommandParam(command, "-U", databaseSettings.getUsername());
        command = addCommandParam(command, "-p", Integer.toString(connUrl.getPort()));
        command = addCommandParam(command, "-d", databaseSettings.getDatabaseName());
        command = addCommandParam(command, "-f", backupFile.getAbsolutePath());

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

    public InputStream createDbDump() {
        List<String> backupCommand = getBackupCommand();
        logger.info("Executing backup command: {}", backupCommand.toString());

        try {
            File backupFile = new File("");
            for (int currentParam = 0; currentParam < backupCommand.size(); currentParam++) {
                if (backupCommand.get(currentParam).equals("-f")) {
                    backupFile = new File(backupCommand.get(currentParam + 1));
                    break;
                }
            }
            Process process = runProcess(backupCommand);

            Thread processErrorStreamReader = new Thread(new ProcessStandartErrorStreamReader(process.getErrorStream()));
            processErrorStreamReader.start();

            Thread processOutputStreamReader = new Thread(new ProcessStandartOutputStreamReader(process.getInputStream()));
            processOutputStreamReader.start();

            process.waitFor();
            process.destroy();

            logger.info("Backup creation process successfully completed");

            return new FileInputStream(backupFile);
        } catch (IOException | InterruptedException ex) {
            throw new RuntimeException("Error occurred while creating postgres database dump", ex);
        }
    }

    public void restoreDbDump(InputStream dump) {
        try {
            List<String> restoreCommand = getRestoreCommand(dump);
            logger.info("Executing restore command: {}", restoreCommand.toString());

            Process process = runProcess(restoreCommand);

            Thread processErrorStreamReader = new Thread(new ProcessStandartErrorStreamReader(process.getErrorStream()));
            processErrorStreamReader.start();

            Thread processOutputStreamReader = new Thread(new ProcessStandartOutputStreamReader(process.getInputStream()));
            processOutputStreamReader.start();

            process.waitFor();
            process.destroy();

            logger.info("Backup restoration process successfully completed");
        } catch (IOException | InterruptedException ex) {
            throw new RuntimeException("Error occurred while restoring postgres database dump", ex);
        }

        logger.info("Database successfully restored. Database: {}", databaseSettings.getDatabaseName());
    }
}