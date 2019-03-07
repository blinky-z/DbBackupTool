package com.example.demo;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.GZIPOutputStream;

public class FileSystemBackupWriter extends BackupWriter {
    private BufferedWriter fileWriter;
    private long currentFileSize;
    private final boolean enableZip;

    FileSystemBackupWriter(String databaseName, long maxFileSizeInBytes, boolean enableZip) {
        super(databaseName, maxFileSizeInBytes);
        this.enableZip = enableZip;
    }

    private void createNewFile() throws IOException {
        SimpleDateFormat date = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss");
        String dateAsString = date.format(new Date());
        File currentFile;
        if (enableZip) {
            currentFile = new File(System.getProperty("user.dir") + File.separator + "backup_" + databaseName + "_"
                    + dateAsString + ".zip");
            GZIPOutputStream zip = new GZIPOutputStream(new FileOutputStream(currentFile));
            fileWriter = new BufferedWriter(new OutputStreamWriter(zip));
        } else {
            currentFile = new File(System.getProperty("user.dir") + File.separator + "backup_" + databaseName + "_"
                    + dateAsString + ".data");
            fileWriter = new BufferedWriter(new FileWriter(currentFile));
        }
        currentFileSize = 0;
    }

    public void write(String data) {
        try {
            if (currentFileSize >= maxChunkSize) {
                fileWriter.close();
                createNewFile();
            }
            if (fileWriter == null) {
                createNewFile();
            }
            fileWriter.write(data);
            fileWriter.newLine();
            currentFileSize += data.getBytes().length;
        } catch (IOException ex) {
            throw new RuntimeException("Error occurred while writing data to file", ex);
        }
    }

    public void close() {
        try {
            fileWriter.close();
        } catch (IOException ex) {
            throw new RuntimeException("Error occurred while closing file", ex);
        }
    }
}
