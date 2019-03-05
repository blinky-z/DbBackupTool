package com.example.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
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

    private String databaseName;
    @Value("${spring.datasource.username}")
    private String databaseUser;
    @Value("${spring.datasource.password}")
    private String databasePassword;

    public void backup() throws IOException, SQLException, InterruptedException {
        SimpleDateFormat date = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss");
        String dateAsString = date.format(new Date());
        databaseName = jdbcTemplate.queryForObject("SELECT current_database()", String.class);
        File backupFilePath = new File(System.getProperty("user.dir") + File.separator + "backup_" + databaseName + "_" +
                dateAsString + ".sql");

        try {
            Process process;
            ProcessBuilder pb;
            List<String> backupCommand = getBackupCommand(backupFilePath);
            infoLogger.info("Executing backup command: " + backupCommand.toString());
            pb = new ProcessBuilder(backupCommand);
            pb.environment().put("PGUSER", databaseUser);
            pb.environment().put("PGPASSWORD", databasePassword);
            process = pb.start();

            BufferedReader errorStreamReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String error;
            while ((error = errorStreamReader.readLine()) != null) {
                errorLogger.error(error);
            }

            process.waitFor();
            process.destroy();

            infoLogger.info("Backup of database " + databaseName + "with timestamp: (" + dateAsString + ") " +
                    "successfully created");
        } catch (IOException | InterruptedException | SQLException ex) {
            errorLogger.error("Error creating backup of database " + databaseName + "with timestamp: (" + dateAsString + "). " +
                    "Error: " + ex.toString());
            throw ex;
        }
    }

    private List<String> getBackupCommand(File backupFilePath) throws SQLException {
        ArrayList<String> command = new ArrayList<>();
        command.add("pg_dump");

        String connUrl = jdbcTemplate.getDataSource().getConnection().getMetaData().getURL();
        String jdbcPrefix = "jdbc:";
        connUrl = connUrl.substring(jdbcPrefix.length());

        URI parsedConnUrl = URI.create(connUrl);
        command = addCommandParam(command, "-h", parsedConnUrl.getHost());
        command = addCommandParam(command, "-p", Integer.toString(parsedConnUrl.getPort()));
        command = addCommandParam(command, "-d", databaseName);
        command = addCommandParam(command, "-f", backupFilePath.getAbsolutePath());
        return command;
    }

    private ArrayList<String> addCommandParam(ArrayList<String> command, String paramName, String paramValue) {
        command.add(paramName);
        if (!paramValue.equals("")) {
            command.add(paramValue);
        }
        return command;
    }
}