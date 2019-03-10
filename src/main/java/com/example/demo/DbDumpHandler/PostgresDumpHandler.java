package com.example.demo.DbDumpHandler;

import com.example.demo.DbBackup;
import com.example.demo.settings.DatabaseSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import com.example.demo.settings.UserSettings;

import java.io.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Component
public class PostgresDumpHandler implements DbDumpHandler {
    @Autowired
    @Qualifier("db-settings")
    private DatabaseSettings databaseSettings;

    private UserSettings userSettings;

    @Autowired
    public void setUserSettings(UserSettings userSettings) {
        this.userSettings = userSettings;
    }

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

        pb = new ProcessBuilder(command);
        pb.environment().put("PGUSER", databaseSettings.getUsername());
        pb.environment().put("PGPASSWORD", databaseSettings.getPassword());
        pb.redirectErrorStream(true);
        process = pb.start();

        return process;
    }

    private List<String> getBackupCommand() {
        ArrayList<String> command = new ArrayList<>();

        URI connUrl = getParsedConnUrl();

        command.add("pg_dump");
        command = addCommandParam(command, "-h", connUrl.getHost());
        command = addCommandParam(command, "-p", Integer.toString(connUrl.getPort()));
        command = addCommandParam(command, "-F", "p");
        command = addCommandParam(command, "-d", userSettings.getDatabaseName());

        return command;
    }

    private List<String> getRestoreCommand() {
        ArrayList<String> command = new ArrayList<>();

        URI connUrl = getParsedConnUrl();

        command.add("pg_restore");
        command = addCommandParam(command, "-h", connUrl.getHost());
        command = addCommandParam(command, "-p", Integer.toString(connUrl.getPort()));
        command = addCommandParam(command, "-d", userSettings.getDatabaseName());

        return command;
    }

    public InputStream createDbDump() {
        List<String> backupCommand = getBackupCommand();
        logger.info("Executing backup command: {}", backupCommand.toString());

        try {
            Process process = runProcess(backupCommand);
//            try (
//                    BufferedReader errorStreamReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))
//            ) {
////                String error;
////                while ((error = errorStreamReader.readLine()) != null) {
////                    logger.error(error);
////                    // TODO: выкидывать ошибку, но error стрим объединен с output стримом, так что я не знаю как проверить ошибки
////                }
//            }
            return process.getInputStream();
        } catch (IOException ex) {
            throw new RuntimeException("Error occurred while creating postgres database dump", ex);
        }
    }

    public void restoreDbDump(InputStream dump) {
        List<String> restoreCommand = getRestoreCommand();
        logger.info("Executing restore command: {}", restoreCommand.toString());

        try {
            Process process = runProcess(restoreCommand);
            try (
                    BufferedReader dumpStreamReader = new BufferedReader(new InputStreamReader(dump));
                    BufferedWriter processStreamWriter = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()))
            ) {
                String currentLine;
                while ((currentLine = dumpStreamReader.readLine()) != null) {
                    processStreamWriter.write(currentLine);
                }
            }

            logger.info("Database successfully restored. Database: {}", userSettings.getDatabaseName());
        } catch (IOException ex) {
            throw new RuntimeException("Error occurred while restoring postgres database dump", ex);
        }
    }
}
