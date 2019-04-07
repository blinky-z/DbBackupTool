package com.blog.entities.backup;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "backup_properties")
public class BackupProperties {
    @Id
    @Column(insertable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(updatable = false)
    private Date date;

    private String backupName;

    @Column(updatable = false)
    @Convert(converter = ProcessorsFieldConverter.class)
    private List<String> processors;

    private String storageSettingsName;

    BackupProperties() {

    }

    public BackupProperties(String backupName, List<String> processors, Date date, String storageSettingsName) {
        this.backupName = backupName;
        this.processors = processors;
        this.date = date;
        this.storageSettingsName = storageSettingsName;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
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

    public List<String> getProcessors() {
        return processors;
    }

    public void setProcessors(List<String> processors) {
        this.processors = processors;
    }

    public String getStorageSettingsName() {
        return storageSettingsName;
    }

    public void setStorageSettingsName(String storageSettingsName) {
        this.storageSettingsName = storageSettingsName;
    }

    @Override
    public String toString() {
        return "BackupProperties{" +
                "id=" + id +
                ", date=" + date +
                ", backupName='" + backupName + '\'' +
                ", processors=" + processors +
                ", storageSettingsName=" + storageSettingsName +
                '}';
    }
}
