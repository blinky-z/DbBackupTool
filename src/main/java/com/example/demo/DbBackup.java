package com.example.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class DbBackup {
    private JdbcTemplate jdbcTemplate;

    private static final Logger infoLogger = LoggerFactory.getLogger(DbBackup.class);
    private static final Logger errorLogger = LoggerFactory.getLogger(DbBackup.class);

    @Autowired
    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Value("${spring.datasource.username}")
    private String databaseUser;
    @Value("${spring.datasource.password}")
    private String databasePassword;

    public void backup(boolean compressData, long maxFileSizeInBytes) throws IOException, SQLException, InterruptedException {
        SimpleDateFormat date = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss");
        String dateAsString = date.format(new Date());
        String databaseName = jdbcTemplate.queryForObject("SELECT current_database()", String.class);

        try {
            Process process;
            ProcessBuilder pb;
            List<String> backupCommand = getBackupCommand(databaseName, compressData);
            infoLogger.info("Executing backup command: {}", backupCommand.toString());
            pb = new ProcessBuilder(backupCommand);
            pb.environment().put("PGUSER", databaseUser);
            pb.environment().put("PGPASSWORD", databasePassword);
            pb.redirectErrorStream(true);
            process = pb.start();

            BufferedReader errorStreamReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String error;
            while ((error = errorStreamReader.readLine()) != null) {
                errorLogger.error(error);
            }

            BackupWriter backupWriter = new BackupWriter(databaseName, maxFileSizeInBytes);

            BufferedReader dumpStreamReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String currentLine;
            while ((currentLine = dumpStreamReader.readLine()) != null) {
                backupWriter.write(currentLine);
            }
            backupWriter.close();

            process.waitFor();
            process.destroy();

            infoLogger.info("Backup of database {} with timestamp: ({}) successfully created", databaseName, dateAsString);
            infoLogger.info("Created files list: {}", backupWriter.getCreatedFiles());
        } catch (IOException | InterruptedException | SQLException ex) {
            errorLogger.error("Error creating backup of database {} with timestamp: ({}). Error: {}", databaseName, dateAsString,
                    ex.toString());
            throw ex;
        }
    }

    private List<String> getBackupCommand(String databaseName, boolean compressData) throws SQLException {
        ArrayList<String> command = new ArrayList<>();

        String compressLevel;
        if (compressData) {
            compressLevel = "6";
        } else {
            compressLevel = "0";
        }

        String connUrl = jdbcTemplate.getDataSource().getConnection().getMetaData().getURL();
        String jdbcPrefix = "jdbc:";
        connUrl = connUrl.substring(jdbcPrefix.length());

        URI parsedConnUrl = URI.create(connUrl);

        command.add("pg_dump");
        command = addCommandParam(command, "-h", parsedConnUrl.getHost());
        command = addCommandParam(command, "-p", Integer.toString(parsedConnUrl.getPort()));
        command = addCommandParam(command, "-F", "p");
        command = addCommandParam(command, "-Z", compressLevel);
        command = addCommandParam(command, "-d", databaseName);

        return command;
    }

    private ArrayList<String> addCommandParam(ArrayList<String> command, String paramName, String paramValue) {
        command.add(paramName);
        if (paramValue != null) {
            command.add(paramValue);
        }
        return command;
    }
}