package com.example.demo.DbDumpHandler;

import com.example.demo.DbBackup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import settings.DatabaseSettings;

import java.io.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Component
public class PostgresDumpHandler implements DbDumpHandler {
    private DatabaseSettings dbSettings;

    @Autowired
    public void setDbSettings(DatabaseSettings dbSettings) {
        this.dbSettings = dbSettings;
    }

    private static final Logger logger = LoggerFactory.getLogger(DbBackup.class);

    private URI getParsedConnUrl() {
        String jdbcPrefix = "jdbc:";
        String cleanConnUrl = dbSettings.getConnectionUrl().substring(jdbcPrefix.length());

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

        pb = new ProcessBuilder(command);
        pb.environment().put("PGUSER", dbSettings.getDatabaseUser());
        pb.environment().put("PGPASSWORD", dbSettings.getDatabasePassword());
        pb.redirectErrorStream(true);
        process = pb.start();

        return process;
    }

    private List<String> getBackupCommand(String databaseName) {
        ArrayList<String> command = new ArrayList<>();

        URI connUrl = getParsedConnUrl();

        command.add("pg_dump");
        command = addCommandParam(command, "-h", connUrl.getHost());
        command = addCommandParam(command, "-p", Integer.toString(connUrl.getPort()));
        command = addCommandParam(command, "-F", "p");
        command = addCommandParam(command, "-d", databaseName);

        return command;
    }

    private List<String> getRestoreCommand(String databaseName) {
        ArrayList<String> command = new ArrayList<>();

        URI connUrl = getParsedConnUrl();

        command.add("pg_restore");
        command = addCommandParam(command, "-h", connUrl.getHost());
        command = addCommandParam(command, "-p", Integer.toString(connUrl.getPort()));
        command = addCommandParam(command, "-d", databaseName);

        return command;
    }

    public InputStream createDbDump() {
        List<String> backupCommand = getBackupCommand(dbSettings.getDatabaseName());
        logger.info("Executing backup command: {}", backupCommand.toString());

        try {
            Process process = runProcess(backupCommand);
            try (
                    BufferedReader errorStreamReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))
            ) {
//                String error;
//                while ((error = errorStreamReader.readLine()) != null) {
//                    logger.error(error);
//                    // TODO: выкидывать ошибку, но error стрим объединен с output стримом, так что я не знаю как проверить ошибки
//                }
            }
            return process.getInputStream();
        } catch (IOException ex) {
            throw new RuntimeException("Error occurred while creating postgres database dump", ex);
        }
    }

    public void restoreDbDump(InputStream dump) {
        List<String> restoreCommand = getRestoreCommand(dbSettings.getDatabaseName());
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
        } catch (IOException ex) {
            throw new RuntimeException("Error occurred while restoring postgres database dump", ex);
        }
    }
}
