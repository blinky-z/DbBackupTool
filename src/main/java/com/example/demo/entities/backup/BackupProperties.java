package com.example.demo.entities.backup;

import com.example.demo.entities.database.Database;
import com.example.demo.entities.storage.Storage;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "backup_properties")
public class BackupProperties {
    @Id
    @Column(insertable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(insertable = false, updatable = false)
    private Date date;

    private String backupName;

    private Boolean compressed;

    private Integer storageSettingsId;

    BackupProperties() {

    }

    public BackupProperties(String backupName, Boolean compressed,
                            Integer storageSettingsId, Integer databaseSettingsId) {
        this.backupName = backupName;
        this.compressed = compressed;
        this.storageSettingsId = storageSettingsId;
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

    public String getBackupName() {
        return backupName;
    }

    public void setBackupName(String backupName) {
        this.backupName = backupName;
    }

    public Boolean getCompressed() {
        return compressed;
    }

    public void setCompressed(Boolean compressed) {
        this.compressed = compressed;
    }

    public Integer getStorageSettingsId() {
        return storageSettingsId;
    }

    public void setStorageSettingsId(Integer storageSettingsId) {
        this.storageSettingsId = storageSettingsId;
    }

    @Override
    public String toString() {
        return "BackupProperties{" +
                "id=" + id +
                ", date=" + date +
                ", backupName='" + backupName + '\'' +
                ", compressed=" + compressed +
                ", storageSettingsId=" + storageSettingsId +
                '}';
    }
}
