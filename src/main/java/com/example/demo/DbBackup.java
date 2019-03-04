package com.example.demo;

import com.fasterxml.jackson.databind.ser.Serializers;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

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

@Service
public class DbBackup {
    private JdbcTemplate jdbcTemplate;

    @Autowired
    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private DatabaseMetaData metadata;
    private CopyManager copyManager;

    private final int COLUMN_NAME = 4;
    private final int TABLE_NAME = 3;

    private List<String> getColumnNames(String tableName) {
        List<String> columnNames = new ArrayList<>();
        try {
            ResultSet columns = metadata.getColumns(null, null, tableName, null);
            while (columns.next()) {
                columnNames.add(columns.getString(COLUMN_NAME));
            }
        } catch (SQLException ex) {
            System.err.println(ex.toString());
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
            System.err.println(ex.toString());
        }
        out.println("\\.");
        out.println();
    }

    public void backupDB() {
        try {
            Connection conn = jdbcTemplate.getDataSource().getConnection();
            metadata = conn.getMetaData();
            copyManager = new CopyManager(conn.unwrap(BaseConnection.class));
        } catch (SQLException ex) {
            System.err.println(ex.toString());
        }

        SimpleDateFormat date = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss");
        String dateAsString = date.format(new Date());
        File backupFilePath = new File(System.getProperty("user.dir") + File.separator + "backup_" +
                dateAsString + ".sql");

        try {
            PrintWriter out = new PrintWriter(backupFilePath);

            ResultSet tables = metadata.getTables(null, null, "%", new String[]{"TABLE"});
            while (tables.next()) {
                copyCurrentTable(tables.getString(TABLE_NAME), out);
            }

            out.close();
            System.out.println("Backup of database with timestamp: (" + dateAsString + ") " +
                    "successfully created");
        } catch (IOException | SQLException ex) {
            System.err.println("Error creating backup of database");
            System.err.println("Error: " + ex.toString());
        }
    }
}