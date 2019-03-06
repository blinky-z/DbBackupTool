package com.example.demo;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class BackupWriter {
    private String databaseName;
    private BufferedWriter writer;
    private long currentFileSize;
    // created files list. Files sorted by creation time
    private List<File> createdFiles;
    // maxFileSize - max file size in bytes. If 0, then write to single file
    private long maxFileSize;

    BackupWriter(String databaseName, long maxFileSizeInBytes) throws IOException {
        this.databaseName = databaseName;
        this.maxFileSize = maxFileSizeInBytes;
        createdFiles = new ArrayList<>();
        createNewFile();
    }

    private void createNewFile() throws IOException {
        SimpleDateFormat date = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss");
        String dateAsString = date.format(new Date());
        File currentFile = new File(System.getProperty("user.dir") + File.separator + "backup" + createdFiles.size()
                + "_" + databaseName + "_" + dateAsString + ".sql");
        writer = new BufferedWriter(new FileWriter(currentFile));
        currentFileSize = 0;
        createdFiles.add(currentFile);
    }

    void write(String line) throws IOException {
        if (currentFileSize >= maxFileSize) {
            writer.close();
            createNewFile();
        }
        writer.write(line);
        writer.newLine();
        currentFileSize += line.getBytes().length;
    }

    void close() throws IOException {
        writer.close();
    }

    List<File> getCreatedFiles() {
        return createdFiles;
    }
}
