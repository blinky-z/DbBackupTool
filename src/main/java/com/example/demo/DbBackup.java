package com.example.demo;

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
    public static final String TABLE_TYPE = "TABLE";
    public static final String SEQUENCE_TYPE = "SEQUENCE";
    private JdbcTemplate jdbcTemplate;

    @Autowired
    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private DatabaseMetaData metadata;
    private CopyManager copyManager;

    // columns ResultSet
    private final int COLUMNS_COLUMN_NAME = 4;

    // tables ResultSet
    private final int TABLES_TABLE_NAME = 3;
    private final int TABLES_SCHEME_NAME = 2;

    private List<String> getColumnNames(String tableName, String scheme) {
        List<String> columnNames = new ArrayList<>();
        try {
            ResultSet columns = metadata.getColumns(null, scheme, tableName, null);
            while (columns.next()) {
                columnNames.add(columns.getString(COLUMNS_COLUMN_NAME));
            }
        } catch (SQLException ex) {
            System.err.println(ex.toString());
        }

        return columnNames;
    }

    private void copyCurrentSequence(String sequenceName, String scheme, PrintWriter out) {

    }

    private void copyCurrentTable(String tableName, String scheme, PrintWriter out) {
        String tableNameWithScheme;
        if (scheme != null) {
            tableNameWithScheme = scheme + "." + tableName;
        } else {
            tableNameWithScheme = tableName;
        }
        out.print("COPY " + tableNameWithScheme + " (");
        List<String> columnNames = getColumnNames(tableName, scheme);
        for (int currentColumn = 0; currentColumn < columnNames.size(); currentColumn++) {
            out.print(columnNames.get(currentColumn));
            if (currentColumn + 1 < columnNames.size()) {
                out.print(", ");
            }
        }
        out.println(") FROM stdin;");
        try {
            copyManager.copyOut("COPY " + tableNameWithScheme + " TO STDOUT", out);
        } catch (java.sql.SQLException | java.io.IOException ex) {
            System.err.println(ex.toString());
        }
        out.println("\\.");
        out.println();
    }

    public void backup() {
        try {
            Connection conn = jdbcTemplate.getDataSource().getConnection();
            metadata = conn.getMetaData();
            copyManager = new CopyManager(conn.unwrap(BaseConnection.class));
        } catch (SQLException ex) {
            System.err.println(ex.toString());
        }

        SimpleDateFormat date = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss");
        String dateAsString = date.format(new Date());
        File backupFilePath = new File(System.getProperty("user.dir") + File.separator + "backup_" + dateAsString + ".sql");

        try {
            PrintWriter out = new PrintWriter(backupFilePath);

            ResultSet tables = metadata.getTables(null, null, "%", new String[]{"TABLE"});
            while (tables.next()) {
                String currentTableType = tables.getString(4);
                if (currentTableType.equals(TABLE_TYPE)) {
                    copyCurrentTable(tables.getString(TABLES_TABLE_NAME), tables.getString(TABLES_SCHEME_NAME), out);
                } else if (currentTableType.equals(SEQUENCE_TYPE)) {
                    copyCurrentSequence(tables.getString(TABLES_TABLE_NAME), tables.getString(TABLES_SCHEME_NAME), out);
                }
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