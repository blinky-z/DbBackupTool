package com.blog.entities.backup;

import com.blog.entities.storage.StorageSettings;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

/**
 * This entity represents created backup properties.
 * <p>
 * This entity contains all required fields that need to access saved backup on storage:
 * {@link #backupName} and {@link #storageSettingsName}.
 * <p>
 * The logic is following:
 * {@link StorageSettings} can be retrieved using *storageSettingsName* and backup can be uploaded/downloaded/deleted
 * passing *backupName* to storage service.
 *
 * @see com.blog.manager.BackupLoadManager#initNewBackupProperties(StorageSettings, List, String)
 */
@Entity
@Table(name = "backup_properties")
public class BackupProperties {
    /**
     * Identifier of each backup properties.
     */
    @Id
    @Column(insertable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * Backup creation time.
     */
    @Column(updatable = false)
    private Date date;

    /**
     * Backup name. Backup name is an identifier how backup can be accessed using storage service.
     */
    private String backupName;

    /**
     * Applied processors on backup.
     * <p>
     * This field automatically converted into single string and back to List by {@link ListFieldConverter} class.
     */
    @Column(updatable = false)
    @Convert(converter = ListFieldConverter.class)
    private List<String> processors;

    /**
     * Identifier of storage where backup is saved.
     */
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
