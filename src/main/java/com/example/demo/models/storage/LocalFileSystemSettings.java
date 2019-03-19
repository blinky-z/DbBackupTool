package com.example.demo.models.storage;

import javax.persistence.*;
import java.util.Date;
import java.util.HashMap;

@Entity
@Table(name = "local_file_system_settings")
public class LocalFileSystemSettings implements StorageSettings {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String backupPath;

    @Column(insertable = false)
    private Date date;

    public String getBackupPath() {
        return backupPath;
    }

    public void setBackupPath(String backupPath) {
        this.backupPath = backupPath;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    @Override
    public StorageType getStorageType() {
        return StorageType.LOCAL_FILE_SYSTEM;
    }

    @Override
    public HashMap<String, String> getProperties() {
        HashMap<String, String> properties = new HashMap<>();

        properties.put("backup path", backupPath);

        return properties;
    }
}
