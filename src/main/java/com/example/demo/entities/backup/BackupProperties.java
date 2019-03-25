package com.example.demo.entities.backup;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "backup_properties")
public class BackupProperties {
    @Id
    @Column(insertable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(updatable = false)
    private Date date;

    private String backupName;

    private Boolean compressed;

    private Integer storageSettingsId;

    BackupProperties() {

    }

    public BackupProperties(String backupName, Boolean compressed, Date date, Integer storageSettingsId) {
        this.backupName = backupName;
        this.compressed = compressed;
        this.date = date;
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
