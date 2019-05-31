package com.blog.entities.backup;

import com.blog.entities.storage.StorageSettings;
import org.jetbrains.annotations.NotNull;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * This entity represents created backup properties.
 * <p>
 * This entity contains all required fields that need to access saved backup on storage:
 * {@link #backupName} and {@link #storageSettingsName}.
 * <p>
 * The logic is following:
 * {@link StorageSettings} can be retrieved using *storageSettingsName* and backup can be uploaded/downloaded/deleted
 * passing *backupName* and *storage settings* to storage service.
 *
 * @see com.blog.manager.BackupPropertiesManager#initNewBackupProperties(StorageSettings, List, String)
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
    private LocalDateTime date;

    /**
     * Backup name. Backup name is an identifier how backup can be accessed using storage service.
     */
    private String backupName;

    /**
     * Applied processors on backup.
     * <p>
     * This field is automatically converted into single string and back to List by {@link StringListToStringFieldConverter} class.
     */
    @Column(updatable = false)
    @Convert(converter = StringListToStringFieldConverter.class)
    private List<String> processors;

    /**
     * Identifier of storage where backup is saved.
     */
    private String storageSettingsName;

    BackupProperties() {

    }

    public BackupProperties(@NotNull String backupName, @NotNull List<String> processors, @NotNull LocalDateTime date,
                            @NotNull String storageSettingsName) {
        this.backupName = Objects.requireNonNull(backupName);
        this.processors = Objects.requireNonNull(processors);
        this.date = Objects.requireNonNull(date);
        this.storageSettingsName = Objects.requireNonNull(storageSettingsName);
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
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
