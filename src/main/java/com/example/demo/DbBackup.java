package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DbBackup {
    @Autowired
    JdbcTemplate jdbcTemplate;

    @Value("${userConfig.database-name}")
    private String databaseName;
    @Value("${spring.datasource.username}")
    private String databaseUser;
    @Value("${spring.datasource.password}")
    private String databasePassword;

    public void backupDB() {
        SimpleDateFormat date = new SimpleDateFormat("dd-MM-yyyy_HH:mm:ss");
        String dateAsString = date.format(new Date());
        File backupFilePath = new File(System.getProperty("user.dir") + File.separator + "backup_" +
                dateAsString + ".sql");

        try {
            Process process;
            ProcessBuilder pb;
            pb = new ProcessBuilder(getBackupCommand(backupFilePath));
            pb.environment().put("PGUSER", databaseUser);
            pb.environment().put("PGPASSWORD", databasePassword);
            process = pb.start();

            BufferedReader errorStreamReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String error;
            while ((error = errorStreamReader.readLine()) != null) {
                System.out.println(error);
            }

            process.waitFor();
            process.destroy();

            System.out.println("Backup of database " + databaseName + "with timestamp: (" + dateAsString + ") " +
                    "successfully created");
        } catch (IOException | InterruptedException ex) {
            System.err.println("Error creating backup of database " + databaseName + "with timestamp: (" + dateAsString + ")");
            System.err.println("Error: " + ex);
            ex.printStackTrace(System.err);
        }
    }

    private List<String> getBackupCommand(File backupFilePath) {
        ArrayList<String> command = new ArrayList<>();
        command.add("pg_dump");
        command = addCommandParam(command, "-h", "localhost");
        command = addCommandParam(command, "-p", "5432");
        command = addCommandParam(command, "-F", "custom");
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
