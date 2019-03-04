package com.example.demo;

import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Component
public class DbBackup {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Connection conn;
    private DatabaseMetaData metadata;
    private CopyManager copyManager;

    private final int COLUMN_NAME = 4;
    private final int TABLE_NAME = 3;

    DbBackup() {
        try {
            conn = jdbcTemplate.getDataSource().getConnection();
            metadata = conn.getMetaData();
            copyManager = new CopyManager((BaseConnection)conn);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private List<String> getColumnNames(String tableName) {
        List<String> columnNames = new ArrayList<>();
        try {
            ResultSet columns = metadata.getColumns(null, null, tableName, null);
            while (columns.next()) {
                columnNames.add(columns.getString(TABLE_NAME));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return columnNames;
    }

    private void copyCurrentTable(String tableName, PrintWriter out) {
        out.print("COPY " + tableName + " (");
        List<String> columnNames = getColumnNames(tableName);
        for (int currentColumn = 0; currentColumn < columnNames.size(); currentColumn++) {
            out.print(columnNames.get(currentColumn));
            if (currentColumn + 1 < columnNames.size()) {
                out.print(", ");
            }
        }
        out.println(") FROM stdin");
        try {
            copyManager.copyOut("COPY " + tableName + " TO STDOUT", out);
        } catch (java.sql.SQLException | java.io.IOException ex) {
            ex.printStackTrace();
        }
        out.println("\\.");
        out.println();
    }

    public void backupDB() {
        SimpleDateFormat date = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss");
        String dateAsString = date.format(new Date());
        File backupFilePath = new File(System.getProperty("user.dir") + File.separator + "backup_" +
                dateAsString + ".sql");

        try {
            PrintWriter out = new PrintWriter(backupFilePath);

            ResultSet tables = metadata.getTables(null, null, "%", null);
            while (tables.next()) {
                copyCurrentTable(tables.getString(COLUMN_NAME), out);
            }

            System.out.println("Backup of database with timestamp: (" + dateAsString + ") " +
                    "successfully created");
        } catch (IOException | SQLException ex) {
            System.err.println("Error creating backup of database");
            System.err.println("Error: " + ex);
            ex.printStackTrace(System.err);
        }
    }
}